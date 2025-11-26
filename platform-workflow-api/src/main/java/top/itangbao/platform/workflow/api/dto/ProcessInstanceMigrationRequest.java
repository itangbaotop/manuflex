package top.itangbao.platform.workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessInstanceMigrationRequest {
    @NotBlank(message = "Source process definition ID cannot be empty")
    private String sourceProcessDefinitionId; // 源流程定义ID

    @NotBlank(message = "Target process definition ID cannot be empty")
    private String targetProcessDefinitionId; // 目标流程定义ID

    @NotNull(message = "Process instance IDs cannot be null")
    private List<String> processInstanceIds; // 要迁移的流程实例ID列表
}
