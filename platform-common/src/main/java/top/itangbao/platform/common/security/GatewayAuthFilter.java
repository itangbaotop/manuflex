package top.itangbao.platform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader("X-Auth-User");
        String rolesHeader = request.getHeader("X-Auth-Roles");
        String originalJwt = request.getHeader("X-Original-JWT");

        log.debug("GatewayAuthFilter - X-Auth-User: {}, X-Auth-Roles: {}, X-Original-JWT: {}", username, rolesHeader, originalJwt);

        if (username != null) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader != null && !rolesHeader.isEmpty() ? rolesHeader.split(",") : new String[0])
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, originalJwt, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication set for user: {} with roles: {}", username, rolesHeader);
        } else {
            log.debug("No X-Auth-User header found, proceeding as unauthenticated.");
        }

        filterChain.doFilter(request, response);
    }
}
