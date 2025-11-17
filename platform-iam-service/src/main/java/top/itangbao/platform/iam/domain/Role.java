package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mf_roles") // 映射到 mf_roles 表
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 例如: ADMIN, TENANT_ADMIN, USER

    @Column(length = 255)
    private String description;

    // 角色与权限是多对多关系
    @ManyToMany(fetch = FetchType.EAGER) // 立即加载权限信息
    @JoinTable(
            name = "mf_role_permissions", // 关联表名
            joinColumns = @JoinColumn(name = "role_id"), // Role 表在关联表中的外键
            inverseJoinColumns = @JoinColumn(name = "permission_id") // Permission 表在关联表中的外键
    )
    private Set<Permission> permissions = new HashSet<>();
}
