package top.itangbao.platform.iam;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.spring.annotation.datasource.EnableAutoDataSourceProxy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@SpringBootApplication
@ComponentScan(basePackages = {"top.itangbao.platform.iam", "top.itangbao.platform.common"})
@EnableDiscoveryClient
@EnableAutoDataSourceProxy
public class PlatformIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformIamServiceApplication.class, args);
    }

    //  定义事务管理器
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
