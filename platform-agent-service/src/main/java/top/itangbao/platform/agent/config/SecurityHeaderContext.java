package top.itangbao.platform.agent.config;

import java.util.Map;

/**
 * 线程上下文工具：专门用于在主线程和子线程之间传递 Feign 所需的 Header
 */
public class SecurityHeaderContext {
    private static final ThreadLocal<Map<String, String>> HEADERS = new ThreadLocal<>();

    public static void set(Map<String, String> headers) {
        HEADERS.set(headers);
    }

    public static Map<String, String> get() {
        return HEADERS.get();
    }

    public static void clear() {
        HEADERS.remove();
    }
}