package top.itangbao.platform.agent.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局缓存：用于跨线程池传递用户信息
 * Key: UserId (或者 TenantId:UserId)
 * Value: Headers
 */
public class UserTokenCache {
    // 使用 ConcurrentHashMap 保证线程安全
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    public static void put(String userId, Map<String, String> headers) {
        if (userId != null && headers != null) {
            CACHE.put(userId, headers);
        }
    }

    public static Map<String, String> get(String userId) {
        return CACHE.get(userId);
    }

    public static void remove(String userId) {
        if (userId != null) {
            CACHE.remove(userId);
        }
    }
}