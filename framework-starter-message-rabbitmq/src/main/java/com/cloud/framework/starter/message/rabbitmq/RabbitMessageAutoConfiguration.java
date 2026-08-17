package com.cloud.framework.starter.message.rabbitmq;

import com.cloud.framework.message.support.MessageConverterNames;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.adapter.AmqpMessageHandlerMethodFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(name = "com.cloud.framework.starter.message.converter.MessageConverterAutoConfiguration")
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(RabbitMessageProperties.class)
public class RabbitMessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitPublisherConfirm rabbitPublisherConfirm(RabbitMessageProperties properties) {
        return new RabbitPublisherConfirm(properties);
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    @ConditionalOnClass(Jackson2JsonMessageConverter.class)
    public MessageConverter rabbitMessageConverter(ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper objectMapperBean = objectMapper.getIfUnique();
        if (objectMapperBean == null) {
            return new Jackson2JsonMessageConverter();
        }
        return new Jackson2JsonMessageConverter(objectMapperBean);
    }

    @Bean
    @ConditionalOnMissingBean(RabbitListenerConfigurer.class)
    public RabbitListenerConfigurer customRabbitListenerConfigurer(
            @Qualifier(MessageConverterNames.DEFAULT)
            ObjectProvider<org.springframework.messaging.converter.MessageConverter> messageConverter,
            ObjectProvider<ConversionService> conversionService,
            BeanFactory beanFactory
    ) {
        return registrar -> {
            AmqpMessageHandlerMethodFactory defaultFactory = new AmqpMessageHandlerMethodFactory();

            Validator validator = registrar.getValidator();
            if (validator != null) {
                defaultFactory.setValidator(validator);
            }

            conversionService.ifUnique(defaultFactory::setConversionService);
            defaultFactory.setBeanFactory(beanFactory);
            messageConverter.ifAvailable(defaultFactory::setMessageConverter);

            List<HandlerMethodArgumentResolver> customArgumentsResolvers =
                    new ArrayList<>(registrar.getCustomMethodArgumentResolvers());
            // CustomPayloadMethodArgumentResolver should be ordered before PayloadMethodArgumentResolver
            defaultFactory.setCustomArgumentResolvers(customArgumentsResolvers);

            defaultFactory.afterPropertiesSet();
            registrar.setMessageHandlerMethodFactory(defaultFactory);
        };
    }

}
