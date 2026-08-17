package com.cloud.framework.starter.webmvc.client;

import java.util.List;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册无请求体客户端上下文参数解析器。 */
public class ClientRequestWebMvcConfigurer implements WebMvcConfigurer {
    private final ClientRequestArgumentResolver clientRequestArgumentResolver;

    public ClientRequestWebMvcConfigurer(ClientRequestArgumentResolver clientRequestArgumentResolver) {
        this.clientRequestArgumentResolver = clientRequestArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientRequestArgumentResolver);
    }
}
