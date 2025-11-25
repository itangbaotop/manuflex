package top.itangbao.platform.workflow.config;

import jakarta.ws.rs.ApplicationPath;
import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;

@Configuration
@ApplicationPath("/engine-rest")
public class CamundaJerseyConfig extends ResourceConfig {

    public CamundaJerseyConfig() {
        // 这里是关键：只要 classpath 中只有 camunda-engine-rest-core-jakarta
        // CamundaRestResources 就会返回 jakarta 版本的资源类
        registerClasses(CamundaRestResources.getResourceClasses());
        register(JacksonFeature.class);
    }
}