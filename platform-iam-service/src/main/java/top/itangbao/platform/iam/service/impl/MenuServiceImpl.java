package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.domain.Menu;
import top.itangbao.platform.iam.repository.MenuRepository;
import top.itangbao.platform.iam.service.MenuService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Override
    public List<Menu> getMenuTree(boolean isAdmin, Set<String> permissions) {
        // 1. 获取所有菜单 (扁平列表)
        List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();

        // 2. 第一轮筛选：找出用户“直接”拥有的菜单 (Directly Accessible)
        // (管理员可见，无权限配置的可见，拥有对应权限的可见)
        Set<Long> visibleMenuIds = new HashSet<>();
        for (Menu menu : allMenus) {
            if (isAdmin || hasPermission(menu, permissions)) {
                visibleMenuIds.add(menu.getId());
            }
        }

        // 3. 核心逻辑：向上递归补全父菜单 (Recursively add parents)
        // 只要子菜单可见，其所有祖先都必须可见，忽略祖先的权限限制
        Map<Long, Menu> menuMap = allMenus.stream().collect(Collectors.toMap(Menu::getId, m -> m));

        // 使用一个新的集合来存储最终可见的ID，避免在遍历中修改源集合
        Set<Long> finalVisibleIds = new HashSet<>(visibleMenuIds);

        for (Long id : visibleMenuIds) {
            Menu current = menuMap.get(id);
            // 向上遍历直到根节点
            while (current != null && current.getParentId() != null && current.getParentId() != 0) {
                Long parentId = current.getParentId();
                if (!finalVisibleIds.contains(parentId)) {
                    finalVisibleIds.add(parentId); // 补全父ID
                }
                current = menuMap.get(parentId); // 继续向上找爷爷
            }
        }

        // 4. 根据最终 ID 列表过滤菜单
        List<Menu> filteredMenus = allMenus.stream()
                .filter(m -> finalVisibleIds.contains(m.getId()))
                .collect(Collectors.toList());

        // 5. 构建树形结构
        return buildTree(filteredMenus);
    }

    // 辅助方法：检查单个菜单权限
    private boolean hasPermission(Menu menu, Set<String> userPermissions) {
        String perm = menu.getPermission();
        // 如果菜单没配权限(null或空)，默认可见；否则检查用户是否有该权限
        return perm == null || perm.trim().isEmpty() || userPermissions.contains(perm);
    }

    private List<Menu> buildTree(List<Menu> menus) {
        Map<Long, Menu> menuMap = new HashMap<>();
        List<Menu> roots = new ArrayList<>();

        // 重置 children，防止 Hibernate 缓存脏数据干扰
        for (Menu m : menus) {
            m.setChildren(new ArrayList<>());
            menuMap.put(m.getId(), m);
        }

        for (Menu m : menus) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                roots.add(m);
            } else {
                Menu parent = menuMap.get(m.getParentId());
                if (parent != null) {
                    parent.getChildren().add(m);
                } else {
                    // 如果找不到父亲(理论上步骤3已补全)，当作根节点或忽略
                    // 这里选择作为根节点展示，防止菜单丢失
                    roots.add(m);
                }
            }
        }
        return roots;
    }

    // --- 其他 CRUD 方法保持不变 ---
    @Override
    public List<Menu> getAllMenus() {
        return menuRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    public Menu createMenu(Menu menu) {
        if (menu.getParentId() == null) menu.setParentId(0L);
        return menuRepository.save(menu);
    }

    @Override
    public Menu updateMenu(Long id, Menu menuDetails) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));
        menu.setName(menuDetails.getName());
        menu.setPath(menuDetails.getPath());
        menu.setIcon(menuDetails.getIcon());
        menu.setPermission(menuDetails.getPermission());
        menu.setSortOrder(menuDetails.getSortOrder());
        menu.setParentId(menuDetails.getParentId());
        menu.setType(menuDetails.getType());
        menu.setComponent(menuDetails.getComponent());
        return menuRepository.save(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }
}