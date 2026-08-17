package com.cloud.framework.starter.autoconfigure.conversion;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.convert.Jsr310Converters;

@AutoConfiguration
@ConditionalOnClass({ConverterRegistry.class, Jsr310Converters.class})
public class Jsr310ConversionAutoConfiguration {
    @Bean
    public BeanPostProcessor jsr310ConverterRegistryPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ConverterRegistry converterRegistry) {
                    Jsr310ConverterRegistrar.register(converterRegistry);
                }
                return bean;
            }
        };
    }
}
