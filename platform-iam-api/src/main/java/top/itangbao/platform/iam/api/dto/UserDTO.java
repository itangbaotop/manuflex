package top.itangbao.platform.iam.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String tenantId;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<RoleDTO> roles;
    private Set<String> permissions; // 保持只返回权限编码
    private Long deptId;
    private String deptName;
}
