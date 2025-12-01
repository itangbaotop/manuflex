package top.itangbao.platform.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {
    private String id; // 前端期望是 string
    private String name;
    private String description;
    private Set<PermissionDTO> permissions = new HashSet<>(); // 包含 PermissionDTO 列表
}
