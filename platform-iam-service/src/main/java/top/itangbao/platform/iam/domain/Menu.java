package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iam_menus")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 父菜单ID，顶级菜单为 0 或 null
    @Column(name = "parent_id")
    private Long parentId;

    // 菜单显示名称
    @Column(nullable = false)
    private String name;

    // 前端路由路径 (如 /system/users)
    private String path;

    // 菜单图标 (如 UserOutlined)
    private String icon;

    // 需要的权限标识 (如 user:read)，为空则代表所有人可见(或仅登录可见)
    private String permission;

    // 排序号
    @Column(name = "sort_order")
    private Integer sortOrder;

    // 菜单类型 (0: 目录, 1: 菜单, 2: 按钮) - 这里主要关注目录和菜单
    private Integer type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}