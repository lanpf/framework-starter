package com.cloud.framework.starter.message.converter;

import com.cloud.framework.message.support.MessageConverterNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, MessageConverter.class})
public class MessageConverterAutoConfiguration {

    @Bean(name = MessageConverterNames.DEFAULT)
    @ConditionalOnMissingBean(name = MessageConverterNames.DEFAULT)
    public MessageConverter defaultMessageConverter(ObjectProvider<ObjectMapper> objectMapper) {
        MessageConverter messageConverter = SpringMessageConverterFactory.create();
        configureJackson(messageConverter, objectMapper);
        if (messageConverter instanceof CompositeMessageConverter compositeMessageConverter) {
            compositeMessageConverter.getConverters().forEach(converter -> configureJackson(converter, objectMapper));
        }
        return messageConverter;
    }

    private void configureJackson(MessageConverter messageConverter, ObjectProvider<ObjectMapper> objectMapper) {
        if (messageConverter instanceof MappingJackson2MessageConverter jacksonMessageConverter) {
            objectMapper.ifUnique(jacksonMessageConverter::setObjectMapper);
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = "messageHandlerMethodFactoryPostProcessor")
    public static BeanPostProcessor messageHandlerMethodFactoryPostProcessor(
            @Qualifier(MessageConverterNames.DEFAULT)
            ObjectProvider<MessageConverter> messageConverter
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof DefaultMessageHandlerMethodFactory messageHandlerMethodFactory) {
                    messageConverter.ifAvailable(messageHandlerMethodFactory::setMessageConverter);
                }
                return bean;
            }
        };
    }
}
