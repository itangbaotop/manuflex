package top.itangbao.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Username or email cannot be empty")
    private String identifier; // 可以是用户名或邮箱

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
