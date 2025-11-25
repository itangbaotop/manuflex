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

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        // 从 Nacos 配置中获取数据库连接信息
        dataSource.setUrl("jdbc:mysql://localhost:3306/manuflex_paas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
        dataSource.setUsername("manuflex_user");
        dataSource.setPassword("manuflex_password");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return dataSource;
    }

    //  定义事务管理器
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
