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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.itangbao.platform.common.util.JwtTokenProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
                        .handle((token, sink) -> {
                            try {
                                // 调用您的优化方法 (确保 JwtTokenProvider 里有这个方法)
                                Claims claims = jwtTokenProvider.validateAndParse(token);
                                if (claims != null) {
                                    sink.next(claims); // 只有不为 null 才向下传递
                                }
                            } catch (Exception e) {
                                // 解析出错，忽略该 Token，视为未登录
                            }
                        })
                        .cast(Claims.class) // 强转类型
                        .map(claims -> {
                            String username = claims.getSubject();

                            // 1. 解析角色
                            String rolesClaim = claims.get("roles", String.class);
                            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                            if (rolesClaim != null && !rolesClaim.isEmpty()) {
                                Arrays.stream(rolesClaim.split(","))
                                        .map(SimpleGrantedAuthority::new)
                                        .forEach(authorities::add);
                            }

                            // 2. 解析权限 (防止 Metadata 403)
                            try {
                                List<String> permissions = claims.get("permissions", List.class);
                                if (permissions != null) {
                                    permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
                                }
                            } catch (Exception e) {
                                // 忽略权限解析错误
                            }

                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

                            // 存入 Details (为了 SuccessHandler 透传 deptId)
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
                    .header("X-User-Accessible-Depts", StringUtils.collectionToCommaDelimitedString(claims.get("accessibleDeptIds", List.class)))
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
        if (claims == null || !claims.containsKey(key)) {
            return "";
        }
        Object val = claims.get(key);
        return val == null ? "" : String.valueOf(val);
    }
}