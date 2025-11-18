package top.itangbao.platform.metadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "top.itangbao.platform.metadata",
        "top.itangbao.platform.common"}
)
public class PlatformMetadataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformMetadataServiceApplication.class, args);
    }

}
