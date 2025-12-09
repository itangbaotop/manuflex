package top.itangbao.platform.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.itangbao.platform.common.security.CustomUserDetails;

public class SecurityUtils {

    /**
     * 获取当前登录用户完整信息
     */
    public static CustomUserDetails getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前租户 ID
     */
    public static String getTenantId() {
        CustomUserDetails user = getLoginUser();
        return user != null ? user.getTenantId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        CustomUserDetails user = getLoginUser();
        return user != null ? user.getUsername() : null;
    }
}