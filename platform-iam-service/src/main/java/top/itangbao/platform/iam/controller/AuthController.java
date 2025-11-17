package top.itangbao.platform.iam.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.iam.dto.LoginRequest;
import top.itangbao.platform.iam.dto.LoginResponse;
import top.itangbao.platform.iam.dto.RegisterRequest;
import top.itangbao.platform.iam.dto.UserDTO;
import top.itangbao.platform.iam.exception.UserAlreadyExistsException;
import top.itangbao.platform.iam.service.UserService;

@RestController
@RequestMapping("/api/iam/auth") // 定义基础路径
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册接口
     * @param request 注册请求体
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDTO registeredUser = userService.registerUser(request);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED); // 返回 201 Created
        } catch (UserAlreadyExistsException e) {
            // 如果用户已存在，返回 409 Conflict
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    /**
     * 用户登录接口
     * @param request 登录请求体
     * @return 登录成功的响应，包含 token 和用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = userService.loginUser(request);
            return ResponseEntity.ok(loginResponse); // 返回 200 OK
        } catch (Exception e) {
            // 这里可以根据具体的异常类型返回不同的 HTTP 状态码
            // 例如，用户名或密码错误返回 401 Unauthorized
            // 用户不存在返回 404 Not Found
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 暂时统一返回 401
        }
    }

    /**
     * 获取当前认证用户信息的接口
     * 只有携带有效 JWT Token 的认证用户才能访问
     * @return 当前认证用户的 UserDTO
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // 确保用户已认证
    public ResponseEntity<UserDTO> getCurrentUser() {
        // 从 SecurityContextHolder 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 这通常不会发生，因为有 @PreAuthorize("isAuthenticated()") 保护
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // 获取用户名 (通常是 UserDetails 对象)
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // 通过用户名获取用户 DTO
        UserDTO userDTO = userService.getUserByUsername(username);
        return ResponseEntity.ok(userDTO);
    }
}
