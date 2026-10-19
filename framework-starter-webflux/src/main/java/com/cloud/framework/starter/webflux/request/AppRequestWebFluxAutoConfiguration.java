package com.cloud.framework.starter.webflux.request;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;

@AutoConfiguration
@ConditionalOnClass(RequestMappingHandlerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class AppRequestWebFluxAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    static AppRequestHandlerAdapterPostProcessor appRequestHandlerAdapterPostProcessor() {
        return new AppRequestHandlerAdapterPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    AppRequestArgumentResolver appRequestArgumentResolver() { return new AppRequestArgumentResolver(); }

    @Bean
    WebFluxConfigurer appRequestWebFluxConfigurer(AppRequestArgumentResolver resolver) {
        return new WebFluxConfigurer() {
            @Override
            public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                configurer.addCustomResolver(resolver);
            }
        };
    }
}
