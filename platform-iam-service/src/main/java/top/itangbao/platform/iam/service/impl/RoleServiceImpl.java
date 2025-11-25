package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.common.exception.ResourceAlreadyExistsException;
import top.itangbao.platform.iam.dto.RolePermissionUpdateRequest;
import top.itangbao.platform.iam.repository.PermissionRepository;
import top.itangbao.platform.iam.repository.RoleRepository;
import top.itangbao.platform.iam.service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public Role createRole(String roleName, String description) {
        if (roleRepository.existsByName(roleName)) {
            throw new ResourceAlreadyExistsException("Role with name " + roleName + " already exists.");
        }
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    @Override
    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByName(roleName);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role assignPermissionsToRole(RolePermissionUpdateRequest request) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + request.getRoleId()));

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> foundPermissions = permissionRepository.findAllById(request.getPermissionIds());
            if (foundPermissions.size() != request.getPermissionIds().size()) {
                // 如果请求的权限ID中有些找不到，可以抛出更具体的异常
                throw new ResourceNotFoundException("One or more permissions not found.");
            }
            permissions.addAll(foundPermissions);
        }

        role.setPermissions(permissions); // 设置新的权限列表
        return roleRepository.save(role);
    }
}
