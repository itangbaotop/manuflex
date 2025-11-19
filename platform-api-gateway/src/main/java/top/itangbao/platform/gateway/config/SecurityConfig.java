package top.itangbao.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager; // 导入 ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import reactor.core.publisher.Mono; // 导入 Mono
import top.itangbao.platform.common.util.JwtTokenProvider;
import top.itangbao.platform.gateway.security.JwtAuthenticationWebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 定义一个简单的 ReactiveAuthenticationManager Bean
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> Mono.just(authentication); // 简单地返回传入的认证对象
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/camunda/**", "/engine-rest/**").permitAll()
                        .pathMatchers("/api/iam/auth/register", "/api/iam/auth/login", "/api/iam/auth/refresh-token").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public JwtAuthenticationWebFilter jwtAuthenticationWebFilter() {
        // 创建 JwtAuthenticationWebFilter 实例，并传入 reactiveAuthenticationManager Bean
        JwtAuthenticationWebFilter authenticationWebFilter = new JwtAuthenticationWebFilter(reactiveAuthenticationManager(), jwtTokenProvider);

        authenticationWebFilter.setRequiresAuthenticationMatcher(
                exchange -> {
                    String path = exchange.getRequest().getPath().value();
                    if (path.startsWith("/api/iam/auth/register") ||
                            path.startsWith("/api/iam/auth/login") ||
                            path.startsWith("/api/iam/auth/refresh-token") ||
                            path.startsWith("/actuator") ||
                            path.startsWith("/swagger-ui") ||
                            path.startsWith("/v3/api-docs") ||
                            path.startsWith("/camunda") ||
                            path.startsWith("/engine-rest")) {
                        return Mono.empty();
                    }
                    return new PathPatternParserServerWebExchangeMatcher("/**").matches(exchange);
                }
        );
        return authenticationWebFilter;
    }
}
