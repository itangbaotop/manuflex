package top.itangbao.platform.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "wf_form_definition")
@Data
public class FormDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String formKey;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "`schema`", columnDefinition = "TEXT")
    private String schema;
    
    private String tenantId;
    
    private LocalDateTime createdAt;
    
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
