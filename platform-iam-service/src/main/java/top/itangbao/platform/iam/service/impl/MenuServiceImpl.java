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
        List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();

        // 1. 过滤权限
        List<Menu> visibleMenus = allMenus.stream()
                .filter(m -> isAdmin || m.getPermission() == null || m.getPermission().isEmpty() || permissions.contains(m.getPermission()))
                .collect(Collectors.toList());

        // 2. 构建树形结构 (Map映射法)
        return buildTree(visibleMenus);
    }

    private List<Menu> buildTree(List<Menu> menus) {
        Map<Long, Menu> menuMap = new HashMap<>();
        List<Menu> roots = new ArrayList<>();

        // 先把所有节点放入 Map，并清空 children (防止Hibernate缓存干扰)
        for (Menu m : menus) {
            m.setChildren(new ArrayList<>());
            menuMap.put(m.getId(), m);
        }

        // 组装父子关系
        for (Menu m : menus) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                roots.add(m);
            } else {
                Menu parent = menuMap.get(m.getParentId());
                if (parent != null) {
                    parent.getChildren().add(m);
                }
            }
        }
        return roots;
    }

    @Override
    public List<Menu> getAllMenus() { return menuRepository.findAllByOrderBySortOrderAsc(); }

    @Override
    public Menu createMenu(Menu menu) {
        if (menu.getParentId() == null) menu.setParentId(0L);
        return menuRepository.save(menu);
    }

    @Override
    public Menu updateMenu(Long id, Menu dto) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menu not found"));
        menu.setName(dto.getName());
        menu.setPath(dto.getPath());
        menu.setIcon(dto.getIcon());
        menu.setPermission(dto.getPermission());
        menu.setComponent(dto.getComponent());
        menu.setSortOrder(dto.getSortOrder());
        menu.setParentId(dto.getParentId());
        return menuRepository.save(menu);
    }

    @Override
    public void deleteMenu(Long id) { menuRepository.deleteById(id); }
}