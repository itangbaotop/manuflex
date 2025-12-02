package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    // 参数变为 code, name, description
    Permission createPermission(String code, String name, String description);

    List<Permission> getAllPermissions();

    // 可选
     Optional<Permission> getPermissionByCode(String code);
}