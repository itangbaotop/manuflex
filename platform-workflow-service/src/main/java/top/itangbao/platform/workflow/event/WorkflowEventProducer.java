package top.itangbao.platform.workflow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import top.itangbao.platform.workflow.api.dto.ProcessEvent;
import top.itangbao.platform.workflow.api.dto.TaskEvent;

@Service
public class WorkflowEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${workflow.events.process-topic}")
    private String processEventsTopic;

    @Value("${workflow.events.task-topic}")
    private String taskEventsTopic;

    @Autowired
    public WorkflowEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProcessEvent(ProcessEvent event) {
        logger.info("Sending ProcessEvent to Kafka topic {}: {}", processEventsTopic, event);
        kafkaTemplate.send(processEventsTopic, event.getProcessInstanceId(), event);
    }

    public void sendTaskEvent(TaskEvent event) {
        logger.info("Sending TaskEvent to Kafka topic {}: {}", taskEventsTopic, event);
        kafkaTemplate.send(taskEventsTopic, event.getTaskId(), event);
    }
}
