package com.cloud.framework.starter.webmvc.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@AutoConfiguration
@ConditionalOnClass({ControllerAdvice.class, RequestBodyAdviceAdapter.class})
public class AppRequestWebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AppRequestBodyAdvice.class)
    public AppRequestBodyAdvice appRequestBodyAdvice() {
        return new AppRequestBodyAdvice();
    }

    @Bean
    @ConditionalOnMissingBean(AppRequestArgumentResolver.class)
    public AppRequestArgumentResolver appRequestArgumentResolver() {
        return new AppRequestArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean(AppRequestWebMvcConfigurer.class)
    public AppRequestWebMvcConfigurer appRequestWebMvcConfigurer(
            AppRequestArgumentResolver appRequestArgumentResolver
    ) {
        return new AppRequestWebMvcConfigurer(appRequestArgumentResolver);
    }
}
