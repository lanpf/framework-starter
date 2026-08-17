package com.cloud.framework.starter.autoconfigure.conversion;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.convert.Jsr310Converters;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Jsr310ConverterRegistrar {
    public static void register(ConverterRegistry registry) {
        Jsr310Converters.getConvertersToRegister().forEach(registry::addConverter);
    }
}
