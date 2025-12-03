package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Menu;

import java.util.List;
import java.util.Set;

public interface MenuService {
    List<Menu> getMenuTree(boolean isAdmin, Set<String> permissions); // 获取树形
    List<Menu> getAllMenus(); // 获取扁平列表(管理用)
    Menu createMenu(Menu menu);
    Menu updateMenu(Long id, Menu menu);
    void deleteMenu(Long id);
}