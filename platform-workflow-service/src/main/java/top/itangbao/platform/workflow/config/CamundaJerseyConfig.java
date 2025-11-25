package top.itangbao.platform.workflow.config;

import jakarta.ws.rs.ApplicationPath;
import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;

@Configuration
@ApplicationPath("/engine-rest") // REST API 根路径
public class CamundaJerseyConfig extends ResourceConfig {

    public CamundaJerseyConfig() {
        registerClasses(CamundaRestResources.getResourceClasses());

        // registerClasses(CamundaRestResources.getConfigurationClasses());

        register(JacksonFeature.class);
    }
}