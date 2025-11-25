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
import java.util.Set;
import java.util.stream.Collectors;

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
     * 为角色分配权限
     * 只有拥有 'ADMIN' 权限的用户才能访问
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

    // 可以在这里添加创建角色、更新角色、删除角色的 API
}
