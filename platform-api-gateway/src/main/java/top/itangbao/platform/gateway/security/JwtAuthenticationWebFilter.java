package top.itangbao.platform.gateway.security;

import io.jsonwebtoken.Claims; // 导入 Claims
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager; // 导入 ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority; // 导入 GrantedAuthority
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
 * JWT 认证 Web 过滤器，用于在 Gateway 层面验证 JWT Token
 */
// @Component // 移除 @Component 注解，因为它现在是通过 @Bean 方法创建的
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 修改构造函数，接受 ReactiveAuthenticationManager
    public JwtAuthenticationWebFilter(ReactiveAuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        super(authenticationManager); // 调用父类构造函数
        this.jwtTokenProvider = jwtTokenProvider;

        setServerAuthenticationConverter(new ServerAuthenticationConverter() {
            @Override
            public Mono<Authentication> convert(ServerWebExchange exchange) {
                return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                        .filter(authHeader -> authHeader.startsWith("Bearer "))
                        .map(authHeader -> authHeader.substring(7))
                        .filter(jwtTokenProvider::validateToken) // 验证 Token 有效性
                        .map(token -> {
                            String username = jwtTokenProvider.getUsernameFromToken(token);
                            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
                            // 确保 "roles" claim 存在且是 String，并处理 null 或空字符串情况
                            String rolesClaim = claims.get("roles", String.class);
                            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesClaim != null && !rolesClaim.isEmpty() ? rolesClaim.split(",") : new String[0])
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());

                            // 构建 Authentication 对象
                            return new UsernamePasswordAuthenticationToken(username, null, authorities);
                        });
            }
        });
        setAuthenticationSuccessHandler((webFilterExchange, authentication) -> {
            // 认证成功后，将认证信息添加到请求头中，转发给下游微服务
            ServerHttpRequest request = webFilterExchange.getExchange().getRequest().mutate()
                    .header("X-Auth-User", authentication.getName())
                    .header("X-Auth-Roles", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(",")))
                    // 可以添加更多信息，例如用户ID，租户ID等
                    .build();
            ServerWebExchange exchange = webFilterExchange.getExchange().mutate().request(request).build();
            return webFilterExchange.getChain().filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))));
        });
        setAuthenticationFailureHandler((webFilterExchange, exception) -> {
            // 认证失败，返回 401 Unauthorized
            ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return Mono.empty();
        });
    }
}
