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
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.dto.LoginRequest;
import top.itangbao.platform.iam.dto.LoginResponse;
import top.itangbao.platform.iam.dto.RegisterRequest;
import top.itangbao.platform.iam.dto.UserDTO;
import top.itangbao.platform.iam.dto.UserUpdateRequest; // 引入
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.common.exception.UserAlreadyExistsException;
import top.itangbao.platform.iam.repository.RoleRepository;
import top.itangbao.platform.iam.repository.UserRepository;
import top.itangbao.platform.iam.service.UserService;
import top.itangbao.platform.common.util.JwtTokenProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService; // 注入
    }

    @Override
    @Transactional
    @GlobalTransactional
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

        // 默认赋予新用户 'USER' 角色
        Optional<Role> defaultRole = roleRepository.findByName("USER");
        if (defaultRole.isPresent()) {
            user.setRoles(Collections.singleton(defaultRole.get()));
        } else {
            // 如果 'USER' 角色不存在，可以抛出异常或创建它
            throw new ResourceNotFoundException("Default role 'USER' not found. Please ensure it exists.");
        }

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

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(); // 生成 Refresh Token

        User user = userRepository.findByUsername(request.getIdentifier())
                .orElseGet(() -> userRepository.findByEmail(request.getIdentifier())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + request.getIdentifier())));

        user.setRefreshToken(refreshToken); // 保存 Refresh Token 到数据库
        userRepository.save(user); // 更新用户

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken) // 返回 Refresh Token
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

        // 检查更新后的用户名或邮箱是否已存在 (排除当前用户自身)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + request.getUsername() + " already exists.");
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        // 使用 BeanUtils.copyProperties 复制非空属性，避免覆盖 null 值
        // 注意：这里需要手动处理密码加密和 tenantId 逻辑
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
        user.setRefreshToken(null); // 清除 Refresh Token
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    // 辅助方法：将 User 实体转换为 UserDTO
    @Override
    public UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(user.getPermissions().stream().map(p -> p.getName()).collect(Collectors.toSet()))
                .build();
    }
}
