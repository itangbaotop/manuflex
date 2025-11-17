package top.itangbao.platform.metadata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// 注意：这里元数据服务不需要完整的 JWT 认证，因为它不负责用户登录。
// 它只需要一个过滤器来验证来自 API Gateway 或其他微服务转发过来的 JWT。
// 暂时我们只配置基础安全，JWT 验证将通过 API Gateway 进行，或通过一个简化的内部 Token 验证。
// 为了简化，这里先允许所有请求，后续再添加 JWT 验证逻辑。

@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态会话
                .authorizeHttpRequests(authorize -> authorize
                        // 允许对 Actuator 端点进行公共访问
                        .requestMatchers("/actuator/**").permitAll()
                        // 允许对 Swagger UI 相关的端点进行公共访问
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 暂时允许所有请求，后续将添加认证
                        .anyRequest().permitAll() // ⚠️ 临时允许所有请求，后续需要修改为 .authenticated() 并添加 JWT 过滤器
                );
        // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class); // JWT 过滤器将在后续添加

        return http.build();
    }
}
