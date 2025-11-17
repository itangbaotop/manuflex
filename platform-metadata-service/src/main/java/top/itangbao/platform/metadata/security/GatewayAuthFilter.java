package top.itangbao.platform.metadata.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义过滤器，用于从 API Gateway 转发的请求头中解析认证信息
 */
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader("X-Auth-User");
        String rolesHeader = request.getHeader("X-Auth-Roles");

        if (username != null) {
            // 如果 Gateway 转发了用户信息，则构建 Authentication 对象
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader != null && !rolesHeader.isEmpty() ? rolesHeader.split(",") : new String[0])
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // 这里我们不需要密码，因为认证已经在 Gateway 完成
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
