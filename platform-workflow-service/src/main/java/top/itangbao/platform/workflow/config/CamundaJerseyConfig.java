package top.itangbao.platform.workflow.config;

import jakarta.ws.rs.ApplicationPath;
import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/engine-rest")
public class CamundaJerseyConfig extends ResourceConfig {

    public CamundaJerseyConfig() {
        // 1. 只注册资源类 (Resources)，这些是核心接口
        registerClasses(CamundaRestResources.getResourceClasses());

        // 2. ❌【关键修改】注释掉下面这行
        // 这些配置类中包含了导致 Jersey 3 状态锁死(IllegalStateException)的旧版组件
        // registerClasses(CamundaRestResources.getConfigurationClasses());

        // 3. 手动注册必要的 Provider
        // 注册 Jackson (JSON支持)
        register(org.glassfish.jersey.jackson.JacksonFeature.class);

        // 如果需要 Camunda 的异常映射，可以尝试单独注册 (如果报错则注释掉)
        // register(org.camunda.bpm.engine.rest.exception.RestExceptionMapper.class);
    }
}