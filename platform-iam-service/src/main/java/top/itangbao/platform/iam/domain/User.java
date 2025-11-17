package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mf_users") // 映射到 mf_users 表
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255) // 密码需要加密存储
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "tenant_id", nullable = false, length = 50) // 租户ID，用于多租户隔离
    private String tenantId;

    @Column(name = "refresh_token", length = 500) // Refresh Token 字段
    private String refreshToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 用户与角色是多对多关系
    @ManyToMany(fetch = FetchType.EAGER) // 立即加载角色信息
    @JoinTable(
            name = "mf_user_roles", // 关联表名
            joinColumns = @JoinColumn(name = "user_id"), // User 表在关联表中的外键
            inverseJoinColumns = @JoinColumn(name = "role_id") // Role 表在关联表中的外键
    )
    private Set<Role> roles = new HashSet<>();

    // 用户与权限是多对多关系 (直接关联，或通过角色间接关联，这里先直接关联，后续可调整)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "mf_user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
