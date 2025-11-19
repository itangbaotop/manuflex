package top.itangbao.platform.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteTaskRequest {
    @NotBlank(message = "Task ID cannot be empty")
    private String taskId;

    private Map<String, Object> variables; // 任务完成时传递的变量
}
