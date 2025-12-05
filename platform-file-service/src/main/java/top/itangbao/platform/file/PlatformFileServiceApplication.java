package top.itangbao.platform.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.file", "top.itangbao.platform.common", "top.itangbao.platform.file.api"}) // 扫描 common 包
@EnableFeignClients(basePackages = "top.itangbao.platform")
@EnableDiscoveryClient
public class PlatformFileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformFileServiceApplication.class, args);
    }
}
