package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.dto.RolePermissionUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    // 创建角色
    Role createRole(String roleName, String description);

    // 更新角色 (新增)
    Role updateRole(Long id, String roleName, String description);

    // 删除角色 (新增)
    void deleteRole(Long id);

    Optional<Role> getRoleByName(String roleName);

    List<Role> getAllRoles();

    Role assignPermissionsToRole(RolePermissionUpdateRequest request);
}