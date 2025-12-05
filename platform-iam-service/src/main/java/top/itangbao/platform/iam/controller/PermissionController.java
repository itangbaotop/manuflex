package top.itangbao.platform.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/api/iam/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 获取系统所有可用权限 (用于前端穿梭框展示)
     * 只有管理员或拥有 'role:read' 权限的用户可访问
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasAuthority('role:read')")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }
}