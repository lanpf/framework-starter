package com.cloud.framework.starter.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@RequiredArgsConstructor
@AutoConfiguration
@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
@EnableConfigurationProperties(JacksonProperties.class)
public class Jackson2ObjectMapperBuilderCustomizerConfiguration {
    private final JacksonProperties jacksonProperties;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer smartDateFormatCustomizer() {
        return this::configureDateFormat;
    }


    private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
        // We support a fully qualified class name extending DateFormat or a date
        // pattern string value
        String dateFormat = this.jacksonProperties.getDateFormat();
        if (dateFormat != null) {
            try {
                Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
                builder.dateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
            }
            catch (ClassNotFoundException ex) {
                ObjectMapper objectMapper = new ObjectMapper();
                SimpleDateFormat simpleDateFormat = new SmartDateFormat(objectMapper.getDateFormat(), dateFormat);
                // Since Jackson 2.6.3 we always need to set a TimeZone (see
                // gh-4170). If none in our properties fallback to the Jackson's
                // default
                TimeZone timeZone = this.jacksonProperties.getTimeZone();
                if (timeZone == null) {
                    timeZone = objectMapper.getSerializationConfig().getTimeZone();
                }
                simpleDateFormat.setTimeZone(timeZone);
                builder.dateFormat(simpleDateFormat);
            }
        }
    }
}
