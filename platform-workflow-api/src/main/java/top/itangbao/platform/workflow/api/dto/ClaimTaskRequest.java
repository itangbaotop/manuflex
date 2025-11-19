package top.itangbao.platform.workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimTaskRequest {
    @NotBlank(message = "Task ID cannot be empty")
    private String taskId;

    @NotBlank(message = "Assignee cannot be empty")
    private String assignee; // 任务执行人
}
