package top.itangbao.platform.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionUpdateRequest {
    @NotNull(message = "Role ID cannot be null")
    private Long roleId;

    @NotNull(message = "Permission IDs cannot be null")
    private Set<Long> permissionIds; // 要分配给角色的权限ID列表
}
