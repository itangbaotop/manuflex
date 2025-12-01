package top.itangbao.platform.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.iam.domain.Menu;
import top.itangbao.platform.iam.dto.MenuDTO;
import top.itangbao.platform.iam.service.MenuService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/iam/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    // 管理员：获取所有菜单列表（扁平结构，用于管理表格）
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Menu>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenus());
    }

    // 获取当前登录用户的菜单树（用于前端渲染左侧侧边栏）
    @GetMapping("/current")
    public ResponseEntity<List<MenuDTO>> getCurrentUserMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 从 Authentication 中提取角色和权限
        // 注意：这里假设您的 UserDetails 实现将 Role 和 Permission 都放入了 Authorities
        // 简单处理：判断是否有 ROLE_ADMIN
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Set<String> permissions = auth.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toSet());

        List<MenuDTO> tree = menuService.getCurrentUserMenuTree(permissions, isAdmin);
        return ResponseEntity.ok(tree);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Menu> createMenu(@RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.createMenu(menu));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Menu> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.updateMenu(id, menu));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}