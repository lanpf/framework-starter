package com.cloud.framework.starter.webflux.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;

@AutoConfiguration
@ConditionalOnClass(RequestMappingHandlerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class WebFluxErrorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    WebFluxExceptionHandler webFluxExceptionHandler() { return new WebFluxExceptionHandler(); }
}
