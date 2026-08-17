package com.cloud.framework.starter.outbox.direct;

import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "framework.outbox.integration-event", name = "mode", havingValue = "direct", matchIfMissing = true)
public class DirectIntegrationEventOutboxConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventOutbox directIntegrationEventOutbox(
            IntegrationEventPublisher integrationEventPublisher
    ) {
        return new DirectIntegrationEventOutbox(integrationEventPublisher);
    }
}
