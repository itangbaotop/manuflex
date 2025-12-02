package top.itangbao.platform.iam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.common.annotation.Log;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.iam.domain.Menu;
import top.itangbao.platform.iam.service.MenuService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/iam/menus")
public class MenuController {
    @Autowired
    private MenuService menuService;

    // 获取当前用户的菜单树 (用于左侧导航)
    @GetMapping("/current")
    @Log(module = "菜单管理", action = "获得菜单树")
    public ResponseEntity<List<Menu>> getCurrentUserMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        // 解析权限
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Set<String> perms = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        return ResponseEntity.ok(menuService.getMenuTree(isAdmin, perms));
    }

    // 获取所有菜单 (用于管理界面)
    @GetMapping
    @Log(module = "菜单管理", action = "获取所有菜单")
    public ResponseEntity<List<Menu>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenus());
    }

    @PostMapping
    @Log(module = "菜单管理", action = "创建菜单")
    public ResponseEntity<Menu> createMenu(@RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.createMenu(menu));
    }

    @PutMapping("/{id}")
    @Log(module = "菜单管理", action = "更新菜单")
    public ResponseEntity<Menu> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.updateMenu(id, menu));
    }

    @DeleteMapping("/{id}")
    @Log(module = "菜单管理", action = "删除菜单")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}