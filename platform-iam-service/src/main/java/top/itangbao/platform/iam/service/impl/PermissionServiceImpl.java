package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.exception.ResourceAlreadyExistsException;
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

    @Override
    @Transactional
    public Permission createPermission(String permissionName, String description) {
        if (permissionRepository.existsByName(permissionName)) {
            throw new ResourceAlreadyExistsException("Permission with name " + permissionName + " already exists.");
        }
        Permission permission = new Permission();
        permission.setName(permissionName);
        permission.setDescription(description);
        return permissionRepository.save(permission);
    }

    @Override
    public Optional<Permission> getPermissionByName(String permissionName) {
        return permissionRepository.findByName(permissionName);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}
