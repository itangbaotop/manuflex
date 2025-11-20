package top.itangbao.platform.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.iam", "top.itangbao.platform.common"})
@EnableDiscoveryClient
public class PlatformIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformIamServiceApplication.class, args);
    }

}
