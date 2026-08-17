package com.cloud.framework.starter.autoconfigure.naming;

import com.cloud.framework.core.naming.NamespaceResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class NamespaceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NamespaceResolver namespaceResolver(Environment environment) {
        return new SpringApplicationNamespaceResolver(environment);
    }
}
