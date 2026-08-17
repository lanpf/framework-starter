package com.cloud.framework.starter.autoconfigure.jackson;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


@RequiredArgsConstructor
@AutoConfiguration
@ConditionalOnClass(JavaTimeModule.class)
@EnableConfigurationProperties(JacksonModuleProperties.class)
public class JacksonModuleConfiguration {
    private final JacksonModuleProperties jacksonModuleProperties;

    @Bean
    @ConditionalOnMissingBean
    public JavaTimeModule javaTimeModule(
            ObjectProvider<LocalDateDeserializer> localDateDeserializer,
            ObjectProvider<LocalDateSerializer> localDateSerializer,
            ObjectProvider<LocalDateTimeDeserializer> localDateTimeDeserializer,
            ObjectProvider<LocalDateTimeSerializer> localDateTimeSerializer,
            ObjectProvider<LocalTimeDeserializer> localTimeDeserializer,
            ObjectProvider<LocalTimeSerializer> localTimeSerializer) {
        JavaTimeModule module = new JavaTimeModule();

        localDateDeserializer.ifUnique(deserializer -> module.addDeserializer(LocalDate.class, deserializer));
        localDateTimeDeserializer.ifUnique(deserializer -> module.addDeserializer(LocalDateTime.class, deserializer));
        localTimeDeserializer.ifUnique(deserializer -> module.addDeserializer(LocalTime.class, deserializer));

        localDateSerializer.ifUnique(serializer -> module.addSerializer(LocalDate.class, serializer));
        localDateTimeSerializer.ifUnique(serializer -> module.addSerializer(LocalDateTime.class, serializer));
        localTimeSerializer.ifUnique(serializer -> module.addSerializer(LocalTime.class, serializer));
        return module;
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateDeserializer smartLocalDateDeserializer() {
        return SmartLocalDateDeserializer.INSTANCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeDeserializer smartLocalDateTimeDeserializer() {
        return SmartLocalDateTimeDeserializer.INSTANCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalTimeDeserializer smartLocalTimeDeserializer() {
        return SmartLocalTimeDeserializer.INSTANCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateSerializer localDateSerializer() {
        return new LocalDateSerializer(DateTimeFormatter.ofPattern(jacksonModuleProperties.getJavaTime().getLocalDatePattern()));
    }
    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeSerializer localDateTimeSerializer() {
        return new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(jacksonModuleProperties.getJavaTime().getLocalDateTimePattern()));
    }
    @Bean
    @ConditionalOnMissingBean
    public LocalTimeSerializer localTimeSerializer() {
        return new LocalTimeSerializer(DateTimeFormatter.ofPattern(jacksonModuleProperties.getJavaTime().getLocalTimePattern()));
    }
}
