package top.itangbao.platform.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "top.itangbao.platform.workflow",
        "top.itangbao.platform.common"
})
public class PlatformWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformWorkflowServiceApplication.class, args);
    }

}
