package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.api.dto.RolePermissionUpdateRequest;
import top.itangbao.platform.iam.domain.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    Role createRole(String roleName, String description, String dataScope);

    Role updateRole(Long id, String roleName, String description, String dataScope);

    // 删除角色 (新增)
    void deleteRole(Long id);

    Optional<Role> getRoleByName(String roleName);

    List<Role> getAllRoles();

    Role assignPermissionsToRole(RolePermissionUpdateRequest request);
}