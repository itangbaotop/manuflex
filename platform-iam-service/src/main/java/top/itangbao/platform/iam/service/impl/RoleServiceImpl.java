package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.api.dto.RolePermissionUpdateRequest;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.common.exception.ResourceAlreadyExistsException;
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
    public Role createRole(String roleName, String description, String dataScope) {
        if (roleRepository.existsByName(roleName)) {
            throw new ResourceAlreadyExistsException("Role exists: " + roleName);
        }
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        role.setDataScope(dataScope != null ? dataScope : "SELF");
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role updateRole(Long id, String roleName, String description, String dataScope) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));

        if (!role.getName().equals(roleName) && roleRepository.existsByName(roleName)) {
            throw new ResourceAlreadyExistsException("Role name exists");
        }

        role.setName(roleName);
        role.setDescription(description);
        if (dataScope != null) {
            role.setDataScope(dataScope);
        }
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found with ID: " + id);
        }
        // TODO: 这里可能需要检查是否有用户关联了该角色，如果有，可能需要先解除关联或禁止删除
        roleRepository.deleteById(id);
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
                throw new ResourceNotFoundException("One or more permissions not found.");
            }
            permissions.addAll(foundPermissions);
        }

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}