package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "mf_departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId; // 父部门ID，0表示根部门

    @Column(name = "sort_order")
    private Integer sortOrder;

    private String leader; // 负责人
    private String phone;  // 联系电话
    private String email;  // 邮箱

    @Column(nullable = false)
    private Boolean status = true; // 状态：正常/停用

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private List<Department> children = new ArrayList<>();
}