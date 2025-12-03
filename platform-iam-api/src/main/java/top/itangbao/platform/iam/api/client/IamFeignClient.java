package top.itangbao.platform.iam.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.itangbao.platform.iam.api.dto.UserDTO;
import java.util.List;

// name 必须与 IAM 服务在 Nacos 注册的 spring.application.name 一致
@FeignClient(name = "platform-iam-service", contextId = "iamClient")
public interface IamFeignClient {

    /**
     * 获取指定部门及其子部门的所有ID
     */
    @GetMapping("/api/iam/departments/{id}/children")
    List<Long> getChildDepartmentIds(@PathVariable("id") Long id);

    /**
     * 根据用户名获取用户信息 (示例)
     */
    @GetMapping("/api/iam/users/by-username")
    UserDTO getUserByUsername(@RequestParam("username") String username);

    // ... 其他需要暴露给微服务调用的接口
}