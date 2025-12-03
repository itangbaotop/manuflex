package top.itangbao.platform.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan; // 导入 ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.data", "top.itangbao.platform.common", "top.itangbao.platform.iam.api"}) // 扫描 common 包
@EnableFeignClients(basePackages = "top.itangbao.platform.metadata.api.client")
@EnableDiscoveryClient
public class PlatformDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformDataServiceApplication.class, args);
    }

}
