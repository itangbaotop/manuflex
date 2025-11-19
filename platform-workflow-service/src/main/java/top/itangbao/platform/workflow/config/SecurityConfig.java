package top.itangbao.platform.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.itangbao.platform.common.security.GatewayAuthFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 允许对 Actuator 端点进行公共访问
                        .requestMatchers("/actuator/**").permitAll()
                        // 允许对 Swagger UI 相关的端点进行公共访问
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 允许 Camunda Webapp 和 REST API 的公共访问 (如果通过 Gateway 认证，这里可以更严格)
                        // Camunda Webapp 默认路径是 /camunda/app/cockpit/default/
                        .requestMatchers("/camunda/**").permitAll() // 暂时允许 Camunda UI 和 REST API 公共访问
                        .requestMatchers("/engine-rest/**").permitAll() // Camunda REST API 默认路径
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(gatewayAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public GatewayAuthFilter gatewayAuthFilter() {
        return new GatewayAuthFilter();
    }
}
