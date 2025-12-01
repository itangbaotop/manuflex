package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mf_permissions") // 映射到 mf_permissions 表
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // <--- **重要修改：将 name 改为 code，例如: user:read, user:write**

    @Column(nullable = false, length = 100) // <--- 新增：权限的显示名称
    private String name; // 例如: 查看用户, 编辑用户

    @Column(length = 255)
    private String description;
}
