package top.itangbao.platform.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.itangbao.platform.common.util.JwtTokenProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证 Web 过滤器
 * 负责解析 Token 并将用户信息（包括 deptId, dataScopes）透传给下游微服务
 */
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationWebFilter(ReactiveAuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        super(authenticationManager);
        this.jwtTokenProvider = jwtTokenProvider;

        // 1. 定义 Token 解析逻辑
        setServerAuthenticationConverter(new ServerAuthenticationConverter() {
            @Override
            public Mono<Authentication> convert(ServerWebExchange exchange) {
                return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                        .filter(authHeader -> authHeader.startsWith("Bearer "))
                        .map(authHeader -> authHeader.substring(7))
                        .filter(jwtTokenProvider::validateToken)
                        .map(token -> {
                            String username = jwtTokenProvider.getUsernameFromToken(token);
                            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

                            String rolesClaim = claims.get("roles", String.class);
                            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesClaim != null && !rolesClaim.isEmpty() ? rolesClaim.split(",") : new String[0])
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());

                            // 构建 Authentication 对象
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

                            // 这样在 SuccessHandler 里才能拿到这些数据
                            auth.setDetails(claims);

                            return auth;
                        });
            }
        });

        // 2. 定义认证成功后的处理逻辑 (透传 Header)
        setAuthenticationSuccessHandler((webFilterExchange, authentication) -> {
            Claims claims = (Claims) authentication.getDetails();

            ServerHttpRequest request = webFilterExchange.getExchange().getRequest().mutate()
                    .header("X-Auth-User", authentication.getName())
                    .header("X-Auth-Roles", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(",")))

                    .header("X-User-Dept-Id", getClaimString(claims, "deptId"))
                    .header("X-User-Data-Scopes", getClaimString(claims, "dataScopes"))
                    .header("X-User-Tenant-Id", getClaimString(claims, "tenantId"))
                    .build();

            ServerWebExchange exchange = webFilterExchange.getExchange().mutate().request(request).build();

            return webFilterExchange.getChain().filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))));
        });

        // 3. 定义失败处理逻辑
        setAuthenticationFailureHandler((webFilterExchange, exception) -> {
            ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return Mono.empty();
        });
    }

    // 辅助方法：安全获取 Claim 字符串 (防止 null 报错)
    private String getClaimString(Claims claims, String key) {
        if (claims == null || claims.get(key) == null) {
            return "";
        }
        return claims.get(key).toString();
    }
}