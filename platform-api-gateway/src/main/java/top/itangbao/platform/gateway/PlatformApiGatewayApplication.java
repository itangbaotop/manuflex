package top.itangbao.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.gateway", "top.itangbao.platform.common"})
public class PlatformApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiGatewayApplication.class, args);
    }

}
