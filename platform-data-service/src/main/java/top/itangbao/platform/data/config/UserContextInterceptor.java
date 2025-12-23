package top.itangbao.platform.data.config;

import com.alibaba.nacos.common.utils.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.itangbao.platform.data.context.UserContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从 Header 读取网关透传的数据
        String username = request.getHeader("X-Auth-User");
        String deptIdStr = request.getHeader("X-User-Dept-Id");
        String dataScopesStr = request.getHeader("X-User-Data-Scopes");
        String deptIdsHeader = request.getHeader("X-User-Accessible-Depts");


        System.out.println("====== INTERCEPTOR IS RUNNING ======");
        System.out.println("User: " + request.getHeader("X-Auth-User"));
        System.out.println("User: " + request.getHeader("X-User-Dept-Id"));

        // 2. 类型转换
        Long deptId = null;
        if (deptIdStr != null && !deptIdStr.isEmpty() && !"null".equals(deptIdStr)) {
            try { deptId = Long.parseLong(deptIdStr); } catch (NumberFormatException e) {}
        }

        Set<String> dataScopes = new HashSet<>();
        if (dataScopesStr != null && !dataScopesStr.isEmpty()) {
            // 是 [SELF, DEPT] 这种格式，简单清洗一下
            String cleanStr = dataScopesStr.replace("[", "").replace("]", "").replace(" ", "");
            if (!cleanStr.isEmpty()) {
                dataScopes.addAll(Arrays.asList(cleanStr.split(",")));
            }
        }

        Set<Long> accessibleDeptIds = new HashSet<>();

        if (StringUtils.hasText(deptIdsHeader)) {
            Arrays.stream(deptIdsHeader.split(","))
                    .map(Long::parseLong)
                    .forEach(accessibleDeptIds::add);
        }

        // 3. 存入上下文
        UserContext.set(username, deptId, dataScopes, accessibleDeptIds);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 4. 清理线程变量，防止内存泄漏
        UserContext.clear();
    }
}