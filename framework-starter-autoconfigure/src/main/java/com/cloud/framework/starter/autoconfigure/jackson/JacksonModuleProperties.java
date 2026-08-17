package com.cloud.framework.starter.autoconfigure.jackson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "spring.jackson.module")
public class JacksonModuleProperties {

    @NestedConfigurationProperty
    private JavaTimeProperties javaTime = new JavaTimeProperties();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JavaTimeProperties {
        private String localDatePattern = SmartDateTimePattern.defaultLocalDatePattern();
        private String localDateTimePattern = SmartDateTimePattern.defaultLocalDateTimePattern();
        private String localTimePattern = SmartDateTimePattern.defaultLocalTimePattern();
    }
}
