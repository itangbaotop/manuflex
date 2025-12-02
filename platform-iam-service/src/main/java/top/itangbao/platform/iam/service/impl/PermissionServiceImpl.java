package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceAlreadyExistsException;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.repository.PermissionRepository;
import top.itangbao.platform.iam.service.PermissionService;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * 创建权限
     * @param code 权限标识 (如 user:read)
     * @param name 显示名称 (如 查看用户)
     * @param description 描述
     * @return 创建的权限对象
     */
    @Override
    @Transactional
    public Permission createPermission(String code, String name, String description) {
        // 使用 code 作为唯一键进行检查
        if (permissionRepository.findByCode(code).isPresent()) {
            throw new ResourceAlreadyExistsException("Permission with code '" + code + "' already exists.");
        }

        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);

        return permissionRepository.save(permission);
    }

    /**
     * 获取所有权限
     */
    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 根据 Code 获取权限 (可选实现，视接口定义而定)
     */
    public Optional<Permission> getPermissionByCode(String code) {
        return permissionRepository.findByCode(code);
    }
}