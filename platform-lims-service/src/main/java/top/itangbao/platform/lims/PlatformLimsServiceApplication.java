package top.itangbao.platform.lims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients; // 启用 Feign 客户端
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "top.itangbao.platform.lims",
        "top.itangbao.platform.common",
        "top.itangbao.platform.metadata.api.client", // 扫描 Metadata API 的 Feign 客户端
        "top.itangbao.platform.data.api.client" // 扫描 Data Service 的 Feign 客户端 (临时)
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "top.itangbao.platform.metadata.api.client",
        "top.itangbao.platform.data.api.client" // 启用 Feign 客户端扫描
})
public class PlatformLimsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformLimsServiceApplication.class, args);
    }

}
