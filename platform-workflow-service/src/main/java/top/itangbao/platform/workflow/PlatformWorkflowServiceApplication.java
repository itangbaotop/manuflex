package top.itangbao.platform.workflow;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableProcessApplication
@SpringBootApplication(exclude = {CamundaBpmRestJerseyAutoConfiguration.class})
@ComponentScan(basePackages = {
        "top.itangbao.platform.workflow",
        "top.itangbao.platform.common",
        "top.itangbao.platform.workflow.api.client" // 扫描 platform-workflow-api 中的 Feign 客户端
})
@EnableDiscoveryClient
//@EnableFeignClients(basePackages = {
//        "top.itangbao.platform.workflow.api.client" // 启用 Feign 客户端扫描
//})
public class PlatformWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformWorkflowServiceApplication.class, args);
    }

}
