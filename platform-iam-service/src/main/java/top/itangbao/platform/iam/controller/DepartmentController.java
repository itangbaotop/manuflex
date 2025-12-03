package top.itangbao.platform.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.common.annotation.Log;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.iam.domain.Department;
import top.itangbao.platform.iam.service.DepartmentService;

import java.util.List;

@RestController
@RequestMapping("/api/iam/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('dept:read') or hasRole('ADMIN')")
    public ResponseEntity<List<Department>> getDepartmentTree(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(departmentService.getDepartmentTree(user.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('dept:write') or hasRole('ADMIN')")
    @Log(module = "部门管理", action = "新建部门")
    public ResponseEntity<Department> createDepartment(@RequestBody Department dept, @AuthenticationPrincipal CustomUserDetails user) {
        dept.setTenantId(user.getTenantId());
        return ResponseEntity.ok(departmentService.createDepartment(dept));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('dept:write') or hasRole('ADMIN')")
    @Log(module = "部门管理", action = "更新部门")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department dept) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dept));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dept:delete') or hasRole('ADMIN')")
    @Log(module = "部门管理", action = "删除部门")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取指定部门及其子部门的所有ID
     * 供微服务内部调用，或前端级联选择使用
     */
    @GetMapping("/{id}/children")
//    @PreAuthorize("hasAuthority('dept:read') or hasRole('ADMIN')") // 或者允许内部调用放行
    public ResponseEntity<List<Long>> getChildDepartmentIds(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(departmentService.getChildDepartmentIds(id, user.getTenantId()));
    }
}