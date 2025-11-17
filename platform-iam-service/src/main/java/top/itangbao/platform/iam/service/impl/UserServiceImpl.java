package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.dto.LoginRequest;
import top.itangbao.platform.iam.dto.LoginResponse;
import top.itangbao.platform.iam.dto.RegisterRequest;
import top.itangbao.platform.iam.dto.UserDTO;
import top.itangbao.platform.iam.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.exception.UserAlreadyExistsException;
import top.itangbao.platform.iam.repository.RoleRepository;
import top.itangbao.platform.iam.repository.UserRepository;
import top.itangbao.platform.iam.service.UserService;
import top.itangbao.platform.iam.util.JwtTokenProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder; // 用于密码加密
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    @Transactional // 事务管理
    public UserDTO registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + request.getUsername() + " already exists.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密存储
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
    public LoginResponse loginUser(LoginRequest request) {
        // 使用 AuthenticationManager 进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        // 将认证信息设置到 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT Token
        String jwt = jwtTokenProvider.generateToken(authentication);

        // 获取认证后的 UserDetails
        User user = (User) userRepository.findByUsername(request.getIdentifier())
                .orElseGet(() -> userRepository.findByEmail(request.getIdentifier())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + request.getIdentifier())));


        return LoginResponse.builder()
                .accessToken(jwt)
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

    // 辅助方法：将 User 实体转换为 UserDTO
    private UserDTO convertToDTO(User user) {
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
