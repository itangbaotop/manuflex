package top.itangbao.platform.iam.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.iam.domain.Department;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.dto.LoginRequest;
import top.itangbao.platform.iam.dto.LoginResponse;
import top.itangbao.platform.iam.dto.PermissionDTO; // 引入 PermissionDTO
import top.itangbao.platform.iam.dto.RegisterRequest;
import top.itangbao.platform.iam.dto.RoleDTO; // 引入 RoleDTO
import top.itangbao.platform.iam.dto.UserDTO;
import top.itangbao.platform.iam.dto.UserUpdateRequest;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.common.exception.UserAlreadyExistsException;
import top.itangbao.platform.iam.repository.DepartmentRepository;
import top.itangbao.platform.iam.repository.RoleRepository;
import top.itangbao.platform.iam.repository.UserRepository;
import top.itangbao.platform.iam.service.UserService;
import top.itangbao.platform.common.util.JwtTokenProvider;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService,
                           DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
//    @GlobalTransactional
    public UserDTO registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + request.getUsername() + " already exists.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        user.setEnabled(true); // 注册时默认启用

        // 传递前端发来的 roles (如果前端没传，resolveRoles 会自动设为默认)
        user.setRoles(resolveRoles(request.getRoles()));

        user.setDeptId(request.getDeptId());

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse loginUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateAccessTokenWithPermissions(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        User user = userRepository.findByUsername(request.getIdentifier())
                .orElseGet(() -> userRepository.findByEmail(request.getIdentifier())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + request.getIdentifier())));

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(convertToDTO(user))
                .build();
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return convertToDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + request.getUsername() + " already exists.");
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getTenantId() != null) {
            user.setTenantId(request.getTenantId());
        }
        if (request.getEnabled() != null) { // 处理 enabled 字段更新
            user.setEnabled(request.getEnabled());
        }

        // 处理角色更新
        if (request.getRoles() != null) {
            user.setRoles(resolveRoles(request.getRoles()));
        }

        if (request.getDeptId() != null) {
            user.setDeptId(request.getDeptId());
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findUserByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void clearRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 加密新密码
        user.setPassword(passwordEncoder.encode(newPassword));

        // 可选：如果实现了"首次登录强制改密"，这里可以设置 user.setPasswordExpired(true);
        // 可选：清除该用户所有的 Refresh Token，强制其重新登录
        user.setRefreshToken(null);

        userRepository.save(user);
    }

    // 辅助方法：将 User 实体转换为 UserDTO
    @Override
    public UserDTO convertToDTO(User user) {
        if (user == null) {
            return null;
        }

        // 转换 roles 为 Set<RoleDTO>
        Set<RoleDTO> roleDTOs = user.getRoles().stream()
                .map(role -> {
                    // 转换 permissions 为 Set<PermissionDTO>
                    Set<PermissionDTO> permissionDTOs = role.getPermissions().stream()
                            .map(perm -> PermissionDTO.builder()
                                    .id(String.valueOf(perm.getId())) // Long to String
                                    .name(perm.getName()) // 对应 Permission 实体中的 name (显示名称)
                                    .code(perm.getCode()) // 对应 Permission 实体中的 code (权限编码)
                                    .description(perm.getDescription())
                                    .build())
                            .collect(Collectors.toSet());

                    return RoleDTO.builder()
                            .id(String.valueOf(role.getId())) // Long to String
                            .name(role.getName())
                            .description(role.getDescription())
                            .permissions(permissionDTOs)
                            .build();
                })
                .collect(Collectors.toSet());

        // 获取所有权限编码
        Set<String> allPermissionCodes = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        // 加上直接分配给用户的权限（如果存在且需要）
        user.getPermissions().stream()
                .map(Permission::getCode)
                .forEach(allPermissionCodes::add);

        String deptName = null;
        if (user.getDeptId() != null) {
            deptName = departmentRepository.findById(user.getDeptId())
                    .map(Department::getName)
                    .orElse("未知部门");
        }

        return UserDTO.builder()
                .id(String.valueOf(user.getId())) // Long to String
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .enabled(user.getEnabled()) // 映射 enabled 字段
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roleDTOs) // 使用转换后的 Set<RoleDTO>
                .deptId(user.getDeptId())
                .deptName(deptName)
                .permissions(allPermissionCodes) // 填充权限编码列表
                .build();
    }

    /**
     * 将角色名列表转换为 Role 实体集合
     * @param roleNames 角色名集合，如 ["ROLE_ADMIN", "ROLE_USER"]
     * @return Role 实体集合
     */
    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            // 如果没有传角色，返回默认 'ROLE_USER' (或者空集合，视业务而定)
            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Default role 'ROLE_USER' not found."));
            return Collections.singleton(defaultRole);
        }

        Set<Role> roles = new HashSet<>();
        roleNames.forEach(roleName -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            roles.add(role);
        });
        return roles;
    }
}
