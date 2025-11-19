package top.itangbao.platform.workflow;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableProcessApplication
@SpringBootApplication
@ComponentScan(basePackages = {
        "top.itangbao.platform.workflow",
        "top.itangbao.platform.common",
        "top.itangbao.platform.workflow.api.client" // 扫描 platform-workflow-api 中的 Feign 客户端
})
//@EnableFeignClients(basePackages = {
//        "top.itangbao.platform.workflow.api.client" // 启用 Feign 客户端扫描
//})
public class PlatformWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformWorkflowServiceApplication.class, args);
    }

}
