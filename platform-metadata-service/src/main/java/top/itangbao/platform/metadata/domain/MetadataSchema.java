package top.itangbao.platform.metadata.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mf_metadata_schemas") // 映射到 mf_metadata_schemas 表
public class MetadataSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 模式名称，例如: Sample, NonConformity, TestData

    @Column(length = 255)
    private String description; // 模式描述

    @Column(name = "tenant_id", nullable = false, length = 50) // 租户ID，用于多租户隔离
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 一个模式包含多个字段 (一对多关系)
    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    // mappedBy 指向 MetadataField 中的 schema 字段
    // cascade = CascadeType.ALL 表示对 MetadataSchema 的操作会级联到 MetadataField
    // orphanRemoval = true 表示当从列表中移除 MetadataField 时，它会被删除
    // fetch = FetchType.EAGER 表示加载 MetadataSchema 时立即加载其所有字段
    private List<MetadataField> fields = new ArrayList<>();
}
