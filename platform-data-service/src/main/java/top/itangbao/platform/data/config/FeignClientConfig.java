package top.itangbao.platform.data.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * Feign 客户端配置，用于在微服务间调用时传递认证信息
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前 SecurityContextHolder 中获取认证信息
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    // 1. 传递 JWT Token (如果存在于 credentials 中)
                    if (authentication.getCredentials() instanceof String jwtToken && StringUtils.hasText(jwtToken)) {
                        template.header("Authorization", "Bearer " + jwtToken);
                        template.header("X-Original-JWT", jwtToken); // 转发原始 JWT
                    }

                    // 2. 传递用户名和角色 (从 SecurityContext 获取)
                    template.header("X-Auth-User", authentication.getName());
                    template.header("X-Auth-Roles", authentication.getAuthorities().stream()
                            .map(grantedAuthority -> grantedAuthority.getAuthority())
                            .collect(Collectors.joining(",")));

                    // TODO: 如果用户ID和租户ID也存储在 Authentication 或 CustomUserDetails 中，也可以转发
                    // 例如：
                    // if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
                    //     template.header("X-Auth-UserId", String.valueOf(customUserDetails.getId()));
                    //     // template.header("X-Auth-TenantId", customUserDetails.getTenantId());
                    // }
                }
            }
        };
    }
}
