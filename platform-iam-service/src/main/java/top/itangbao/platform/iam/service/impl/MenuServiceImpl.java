package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.domain.Menu;
import top.itangbao.platform.iam.dto.MenuDTO;
import top.itangbao.platform.iam.repository.MenuRepository;
import top.itangbao.platform.iam.service.MenuService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

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
        
        return menuRepository.save(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        // 实际生产中应检查是否有子菜单，如果有则禁止删除或级联删除
        menuRepository.deleteById(id);
    }

    @Override
    public List<MenuDTO> getCurrentUserMenuTree(Set<String> userPermissions, boolean isAdmin) {
        List<Menu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        
        // 1. 过滤权限
        List<Menu> visibleMenus = allMenus.stream()
                .filter(menu -> {
                    if (isAdmin) return true; // 管理员看所有
                    if (menu.getPermission() == null || menu.getPermission().isEmpty()) return true; // 无需权限的菜单
                    return userPermissions.contains(menu.getPermission()); // 必须拥有对应权限
                })
                .collect(Collectors.toList());

        // 2. 构建树形结构
        return buildMenuTree(visibleMenus);
    }

    private List<MenuDTO> buildMenuTree(List<Menu> menus) {
        List<MenuDTO> dtoList = menus.stream().map(m -> {
            MenuDTO dto = new MenuDTO();
            BeanUtils.copyProperties(m, dto);
            return dto;
        }).collect(Collectors.toList());

        Map<Long, MenuDTO> dtoMap = dtoList.stream().collect(Collectors.toMap(MenuDTO::getId, m -> m));
        List<MenuDTO> tree = new ArrayList<>();

        for (MenuDTO node : dtoList) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                tree.add(node);
            } else {
                MenuDTO parent = dtoMap.get(node.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(node);
                }
            }
        }
        return tree;
    }
}