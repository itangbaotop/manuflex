package top.itangbao.platform.lims.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription; // 导入
import org.camunda.bpm.client.task.ExternalTask; // 导入
import org.camunda.bpm.client.task.ExternalTaskHandler; // 导入
import org.camunda.bpm.client.task.ExternalTaskService; // 导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@ExternalTaskSubscription("sample-topic") // 订阅名为 "sample-topic" 的外部任务
public class SampleExternalTaskWorker implements ExternalTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(SampleExternalTaskWorker.class);

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        logger.info("Executing external task for topic: {}", externalTask.getTopicName());
        logger.info("Process instance ID: {}", externalTask.getProcessInstanceId());
        logger.info("Task ID: {}", externalTask.getId());

        // 获取流程变量
        String requester = externalTask.getVariable("requester");
        Integer amount = externalTask.getVariable("amount");

        logger.info("Requester: {}, Amount: {}", requester, amount);

        // 模拟业务逻辑
        boolean success = (amount != null && amount > 0); // 假设金额大于0则成功

        Map<String, Object> processVariables;
        if (success) {
            processVariables = Collections.singletonMap("externalTaskResult", "Success");
            logger.info("External task completed successfully for process instance: {}", externalTask.getProcessInstanceId());
            externalTaskService.complete(externalTask, processVariables); // 完成任务并传递变量
        } else {
            processVariables = Collections.singletonMap("externalTaskError", "Invalid amount");
            logger.warn("External task failed for process instance: {}. Reason: Invalid amount", externalTask.getProcessInstanceId());
            // 失败任务，可以重试或抛出 BPMN 错误
            externalTaskService.handleFailure(externalTask, "Invalid amount", "Amount must be greater than 0", 0, 0); // 失败并传递错误信息
        }
    }
}
