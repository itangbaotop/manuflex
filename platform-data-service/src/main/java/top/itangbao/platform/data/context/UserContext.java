package top.itangbao.platform.data.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 线程隔离的用户上下文，用于在 Service 层获取当前用户信息
 */
public class UserContext {
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentDeptId = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> currentDataScopes = new ThreadLocal<>();
    private static final ThreadLocal<Set<Long>> currentAccessibleDeptIds = new ThreadLocal<>();

    public static void set(String username, Long deptId, Set<String> dataScopes, Set<Long> accessibleDeptIds) {
        currentUsername.set(username);
        currentDeptId.set(deptId);
        currentDataScopes.set(dataScopes);
        currentAccessibleDeptIds.set(accessibleDeptIds);
    }

    public static String getUsername() { return currentUsername.get(); }

    public static Long getDeptId() { return currentDeptId.get(); }

    public static Set<String> getDataScopes() {
        return currentDataScopes.get() == null ? Collections.emptySet() : currentDataScopes.get();
    }

    public static Set<Long> getAccessibleDeptIds() {
        return currentAccessibleDeptIds.get() == null ? Collections.emptySet() : currentAccessibleDeptIds.get();
    }

    public static void clear() {
        currentUsername.remove();
        currentDeptId.remove();
        currentDataScopes.remove();
        currentAccessibleDeptIds.remove();
    }
}