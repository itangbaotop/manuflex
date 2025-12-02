package top.itangbao.platform.iam.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.dto.RolePermissionUpdateRequest;
import top.itangbao.platform.iam.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/iam/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 获取所有角色
     * @return 角色列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('role:read') or hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * 创建角色
     * @param role 角色信息
     * @return 创建后的角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:write') or hasRole('ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        // 这里简单复用 Role 实体接收参数，也可以定义 CreateRoleRequest DTO
        Role createdRole = roleService.createRole(role.getName(), role.getDescription());
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    /**
     * 更新角色
     * @param id 角色ID
     * @param role 更新信息
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:write') or hasRole('ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        Role updatedRole = roleService.updateRole(id, role.getName(), role.getDescription());
        return ResponseEntity.ok(updatedRole);
    }

    /**
     * 删除角色
     * @param id 角色ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 为角色分配权限
     * 只有拥有 'role:assign_permission' 权限或 'ADMIN' 角色的用户才能访问
     * @param roleId 角色ID
     * @param request 角色权限更新请求
     * @return 更新后的角色信息
     */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permission') or hasRole('ADMIN')")
    public ResponseEntity<Role> assignPermissionsToRole(@PathVariable Long roleId, @Valid @RequestBody RolePermissionUpdateRequest request) {
        // 确保请求中的 roleId 与路径变量一致
        if (!roleId.equals(request.getRoleId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Role updatedRole = roleService.assignPermissionsToRole(request);
        return ResponseEntity.ok(updatedRole);
    }
}