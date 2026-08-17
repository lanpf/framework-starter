package com.cloud.framework.starter.webmvc.cors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnClass({WebMvcConfigurer.class, CorsFilter.class})
@EnableConfigurationProperties(WebMvcCorsProperties.class)
@ConditionalOnProperty(prefix = "framework.webmvc.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebMvcCorsAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public CorsConfigurationSource corsConfigurationSource(WebMvcCorsProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration(properties));
        return source;
    }

    private CorsConfiguration corsConfiguration(WebMvcCorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());
        configuration.setExposedHeaders(properties.getExposedHeaders());
        configuration.setAllowCredentials(properties.isAllowCredentials());
        configuration.setMaxAge(properties.getMaxAge());
        return configuration;
    }
}
