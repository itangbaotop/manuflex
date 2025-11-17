package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    Permission createPermission(String permissionName, String description);
    Optional<Permission> getPermissionByName(String permissionName);
    List<Permission> getAllPermissions();
    // 更多方法如更新权限、删除权限等
}
