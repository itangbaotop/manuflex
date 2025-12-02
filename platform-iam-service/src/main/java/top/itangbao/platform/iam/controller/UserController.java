package top.itangbao.platform.iam.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 @PreAuthorize
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.common.annotation.Log;
import top.itangbao.platform.iam.dto.UserDTO;
import top.itangbao.platform.iam.dto.UserUpdateRequest;
import top.itangbao.platform.iam.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iam/users") // 定义用户管理的基础路径
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取所有用户
     * 只有拥有 'ADMIN' 角色的用户才能访问
     * @return 所有用户的列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read') or hasRole('ADMIN')") // 只有 ADMIN 角色才能访问
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * 根据 ID 获取单个用户
     * 只有拥有 'ADMIN' 角色或用户自身才能访问
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id") // ADMIN 或用户自身
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 更新用户信息
     * 只有拥有 'ADMIN' 角色或用户自身才能访问
     * @param id 用户ID
     * @param request 更新请求体
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id") // ADMIN 或用户自身
    @Log(module = "用户管理", action = "更新用户")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 删除用户
     * 只有拥有 'ADMIN' 角色的用户才能访问
     * @param id 用户ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色才能访问
    @Log(module = "用户管理", action = "删除用户")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 返回 204 No Content
    }

    /**
     * 管理员重置用户密码
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('user:write') or hasRole('ADMIN')")
    @Log(module = "用户管理", action = "重置密码")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        userService.resetPassword(id, newPassword);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
