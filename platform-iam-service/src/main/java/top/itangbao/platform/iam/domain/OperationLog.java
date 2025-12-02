package top.itangbao.platform.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_oper_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String module;    // 模块名称 (如：用户管理)
    private String action;    // 操作类型 (如：新增、修改)

    @Column(length = 2000)
    private String description; // 操作描述/参数

    private String username;  // 操作人账号
    private String userIp;    // 操作人IP

    private String status;    // 状态 (SUCCESS/FAIL)

    @Column(length = 2000)
    private String errorMsg;  // 错误信息

    private Long executionTime; // 执行时长(ms)

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}