package top.itangbao.platform.agent.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Map<String, String> asyncHeaders = SecurityHeaderContext.get();
                if (asyncHeaders != null) {
                    asyncHeaders.forEach(template::header);
                    return;
                }

                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 1. 透传 Authorization (JWT)
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }

                    // 2. 透传网关解析后的用户信息头 (X-Auth-*, X-User-*)
                    // 这些头是我们在 GatewayAuthFilter 中设置的，用于传递租户、用户ID、权限等
                    copyHeader(request, template, "X-Auth-User");
                    copyHeader(request, template, "X-Auth-Roles");
                    copyHeader(request, template, "X-User-Dept-Id");
                    copyHeader(request, template, "X-User-Data-Scopes");
                    copyHeader(request, template, "X-User-Tenant-Id");
                }
            }
        };
    }

    private void copyHeader(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null) {
            template.header(headerName, value);
        }
    }
}