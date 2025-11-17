package top.itangbao.platform.data.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${metadata.service.url}")
    private String metadataServiceBaseUrl;

    @Bean
    public WebClient metadataServiceWebClient() {
        return WebClient.builder()
                .baseUrl(metadataServiceBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 可以在这里添加默认的认证头，例如在微服务间使用内部Token
                .build();
    }

    // 如果需要调用其他服务，可以定义其他 WebClient Bean
}
