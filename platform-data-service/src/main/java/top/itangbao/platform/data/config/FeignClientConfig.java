package top.itangbao.platform.data.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

/**
 * Feign 客户端配置
 * 作用：拦截所有 Feign 发出的请求，自动把当前 HTTP 请求的 Header（如 Token）透传过去
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 1. 获取当前进来的 HTTP 请求上下文
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 2. 透传 Authorization (Token) - 这是解决 "Access Denied" 的关键
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }

                    // 3. 透传 Gateway 解析好的用户信息 (X-Auth-User 等)
                    // 这样下游 Metadata 服务如果需要用户名或角色，也能直接拿到
                    copyHeader(request, template, "X-Auth-User");
                    copyHeader(request, template, "X-Auth-Roles");

                    // 4. 透传我们自定义的数据权限 Header (P3阶段核心)
                    copyHeader(request, template, "X-User-Dept-Id");
                    copyHeader(request, template, "X-User-Data-Scopes");
                    copyHeader(request, template, "X-User-Tenant-Id");
                }
            }
        };
    }

    /**
     * 辅助方法：复制 Header
     */
    private void copyHeader(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null) {
            template.header(headerName, value);
        }
    }
}