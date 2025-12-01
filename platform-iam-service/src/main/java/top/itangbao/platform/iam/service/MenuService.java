package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Menu;
import java.util.List;
import java.util.Set;

public interface MenuService {
    List<Menu> getAllMenus();
    Menu createMenu(Menu menu);
    Menu updateMenu(Long id, Menu menu);
    void deleteMenu(Long id);
    
    // 获取当前用户可见的菜单树
    List<MenuDTO> getCurrentUserMenuTree(Set<String> userPermissions, boolean isAdmin);
}