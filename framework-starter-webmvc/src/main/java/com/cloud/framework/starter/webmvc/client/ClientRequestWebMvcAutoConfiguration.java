package com.cloud.framework.starter.webmvc.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@AutoConfiguration
@ConditionalOnClass({ControllerAdvice.class, RequestBodyAdviceAdapter.class})
public class ClientRequestWebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ClientRequestBodyAdvice.class)
    public ClientRequestBodyAdvice clientRequestBodyAdvice() {
        return new ClientRequestBodyAdvice();
    }

    @Bean
    @ConditionalOnMissingBean(ClientRequestArgumentResolver.class)
    public ClientRequestArgumentResolver clientRequestArgumentResolver() {
        return new ClientRequestArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean(ClientRequestWebMvcConfigurer.class)
    public ClientRequestWebMvcConfigurer clientRequestWebMvcConfigurer(
            ClientRequestArgumentResolver clientRequestArgumentResolver
    ) {
        return new ClientRequestWebMvcConfigurer(clientRequestArgumentResolver);
    }
}
