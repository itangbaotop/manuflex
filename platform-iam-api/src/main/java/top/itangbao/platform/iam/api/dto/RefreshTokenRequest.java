package top.itangbao.platform.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
}
