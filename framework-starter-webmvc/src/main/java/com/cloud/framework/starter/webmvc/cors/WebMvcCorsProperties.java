package com.cloud.framework.starter.webmvc.cors;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "framework.webmvc.cors")
public class WebMvcCorsProperties {
    private boolean enabled = true;

    private final List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

    private final List<String> allowedMethods = new ArrayList<>(List.of("*"));

    private final List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private final List<String> exposedHeaders = new ArrayList<>(List.of("*"));

    private boolean allowCredentials = true;

    @PositiveOrZero
    private Long maxAge = 3600L;
}
