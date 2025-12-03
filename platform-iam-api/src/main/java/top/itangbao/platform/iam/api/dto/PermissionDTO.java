package top.itangbao.platform.iam.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDTO {
    private String id; // 前端期望是 string
    private String name; // <--- 对应 Permission 实体中的 name (显示名称)
    private String code; // <--- 对应 Permission 实体中的 code (权限编码)
    private String description;
}
