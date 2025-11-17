package top.itangbao.platform.iam.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.dto.*;
import top.itangbao.platform.iam.service.UserService;
import top.itangbao.platform.common.util.JwtTokenProvider;

@RestController
@RequestMapping("/api/iam/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户注册接口
     * @param request 注册请求体
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegisterRequest request) {
        UserDTO registeredUser = userService.registerUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED); // 返回 201 Created
        // UserAlreadyExistsException 将被 GlobalExceptionHandler 捕获并返回 409
    }

    /**
     * 用户登录接口
     * @param request 登录请求体
     * @return 登录成功的响应，包含 token 和用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = userService.loginUser(request);
        return ResponseEntity.ok(loginResponse); // 返回 200 OK
        // 登录失败的异常（如用户名不存在、密码错误）将由 GlobalExceptionHandler 捕获并处理
    }

    /**
     * 使用 Refresh Token 获取新的 Access Token
     * @param request 包含 Refresh Token 的请求体
     * @return 包含新 Access Token 和 Refresh Token 的响应
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshAccessToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // 验证 Refresh Token 的有效性
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // Refresh Token 无效或已过期
        }

        // 从数据库查找持有该 Refresh Token 的用户
        User user = userService.findUserByRefreshToken(requestRefreshToken) // 需要在 UserService 中添加这个方法
                .orElseThrow(() -> new top.itangbao.platform.iam.exception.ResourceNotFoundException("Invalid Refresh Token"));

        // 验证 Refresh Token 是否与数据库中的匹配
        if (!user.getRefreshToken().equals(requestRefreshToken)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // Refresh Token 不匹配
        }

        // 如果 Refresh Token 有效且匹配，则生成新的 Access Token 和 Refresh Token
        // 注意：这里需要重新构建 Authentication 对象来生成新的 Access Token
        // 简单起见，我们先通过用户名获取 UserDetails 来构建 Authentication
        UserDetails userDetails = userService.loadUserByUsername(user.getUsername()); // 假设 UserService 也能加载 UserDetails
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(); // 生成新的 Refresh Token

        // 更新数据库中的 Refresh Token
        user.setRefreshToken(newRefreshToken);
        userService.saveUser(user); // 需要在 UserService 中添加这个方法

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userService.convertToDTO(user)) // 假设 convertToDTO 是 public 的或在 UserService 中
                .build());
    }


    /**
     * 获取当前认证用户信息的接口
     * 只有携带有效 JWT Token 的认证用户才能访问
     * @return 当前认证用户的 UserDTO
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 这里的 null 检查和 UNAUTHORIZED 返回可以移除，因为 @PreAuthorize 已经处理了未认证的情况
        // 但为了代码健壮性，暂时保留，或者简化为直接获取 principal

        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        UserDTO userDTO = userService.getUserByUsername(username);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * 用户登出接口 (撤销 Refresh Token)
     * @return 无内容响应
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        userService.clearRefreshToken(username); // 清除用户的 Refresh Token
        SecurityContextHolder.clearContext(); // 清除 SecurityContext

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
