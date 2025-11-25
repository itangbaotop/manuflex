package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.dto.RolePermissionUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    Role createRole(String roleName, String description);
    Optional<Role> getRoleByName(String roleName);
    List<Role> getAllRoles();
    Role assignPermissionsToRole(RolePermissionUpdateRequest request);
    // 更多方法如更新角色、删除角色、为角色分配权限等
}
