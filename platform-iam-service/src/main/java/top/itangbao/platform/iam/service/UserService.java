package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.dto.LoginRequest;
import top.itangbao.platform.iam.dto.LoginResponse;
import top.itangbao.platform.iam.dto.RegisterRequest;
import top.itangbao.platform.iam.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO registerUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    List<UserDTO> getAllUsers();
    // 更多方法如更新用户、删除用户、分配角色等
}
