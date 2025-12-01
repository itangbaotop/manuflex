package top.itangbao.platform.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.itangbao.platform.iam.domain.Role;
import top.itangbao.platform.iam.domain.Permission;

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

    public static UserDTO fromEntity(top.itangbao.platform.iam.domain.User user) {
        if (user == null) {
            return null;
        }

        Set<RoleDTO> roleDTOs = user.getRoles().stream()
                .map(role -> {
                    Set<PermissionDTO> permissionDTOs = role.getPermissions().stream()
                            .map(perm -> PermissionDTO.builder()
                                    .id(String.valueOf(perm.getId()))
                                    .name(perm.getName())
                                    .code(perm.getCode())
                                    .description(perm.getDescription())
                                    .build())
                            .collect(Collectors.toSet());

                    return RoleDTO.builder()
                            .id(String.valueOf(role.getId()))
                            .name(role.getName())
                            .description(role.getDescription())
                            .permissions(permissionDTOs)
                            .build();
                })
                .collect(Collectors.toSet());

        Set<String> permissionCodes = user.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return UserDTO.builder()
                .id(String.valueOf(user.getId()))
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roleDTOs)
                .permissions(permissionCodes)
                .build();
    }
}
