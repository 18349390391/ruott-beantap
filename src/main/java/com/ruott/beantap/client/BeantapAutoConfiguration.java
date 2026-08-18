package com.ruott.beantap.client;

import com.ruott.beantap.server.BeantapServerLifecycle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

public class BeantapAutoConfiguration {

    @Bean
    public VertHttpClient beantapEndpoint(ApplicationContext context) {
        return new VertHttpClient(context);
    }

    @Bean
    public BeantapServerLifecycle beantapServerLifecycle(VertHttpClient vertHttpClient) {
        return new BeantapServerLifecycle(vertHttpClient);
    }
}
