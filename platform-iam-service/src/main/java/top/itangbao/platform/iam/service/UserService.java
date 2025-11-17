package top.itangbao.platform.iam.service;

import org.springframework.security.core.userdetails.UserDetails;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.dto.*;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDTO registerUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UserUpdateRequest request); // 新增
    void deleteUser(Long id); // 新增

    Optional<User> findUserByRefreshToken(String refreshToken); // 根据 Refresh Token 查找用户
    void saveUser(User user); // 保存用户 (用于更新 Refresh Token)
    void clearRefreshToken(String username); // 清除 Refresh Token
    UserDetails loadUserByUsername(String username); // 方便 AuthController 调用
    UserDTO convertToDTO(User user); // 方便 AuthController 调用
    // 更多方法如更新用户、删除用户、分配角色等
}
