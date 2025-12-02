package top.itangbao.platform.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password; // 允许更新密码，但通常会是单独的密码重置接口

    @Size(max = 50, message = "Tenant ID cannot exceed 50 characters")
    private String tenantId; // 允许更新租户ID，但在实际业务中可能受限

    private Boolean enabled = true;

    private Set<String> roles;
}
