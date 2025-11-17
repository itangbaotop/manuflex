package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.exception.ResourceAlreadyExistsException;
import top.itangbao.platform.iam.repository.RoleRepository;
import top.itangbao.platform.iam.service.RoleService;

import java.util.List;
import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
}
