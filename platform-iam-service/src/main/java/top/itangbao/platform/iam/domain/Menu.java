package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "iam_menus")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId; // 父菜单ID，0表示顶级

    @Column(nullable = false)
    private String name;

    private String path; // 前端路由
    private String icon; // 图标名称
    private String permission; // 权限标识
    // e.g. "UserPage", "RolePage", "DynamicCRUD"
    private String component;

    @Column(name = "sort_order")
    private Integer sortOrder;

    // 类型: 0=目录, 1=菜单, 2=按钮
    private Integer type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 瞬态字段，不映射到数据库，用于返回树形结构
    @Transient
    private List<Menu> children = new ArrayList<>();

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}