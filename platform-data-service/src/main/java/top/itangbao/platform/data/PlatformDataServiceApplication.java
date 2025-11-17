package top.itangbao.platform.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan; // 导入 ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.data", "top.itangbao.platform.common"}) // 扫描 common 包
public class PlatformDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformDataServiceApplication.class, args);
    }

}
