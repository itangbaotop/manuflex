package top.itangbao.platform.iam.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken; // 新增 Refresh Token
    private String tokenType = "Bearer";
    private UserDTO user;
}
