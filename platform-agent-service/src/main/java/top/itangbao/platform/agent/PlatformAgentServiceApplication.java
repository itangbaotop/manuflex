package top.itangbao.platform.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"top.itangbao.platform.agent", "top.itangbao.platform.common", "top.itangbao.platform.agent.api"})
@EnableFeignClients(basePackages = "top.itangbao.platform")
public class PlatformAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformAgentServiceApplication.class, args);
    }
}