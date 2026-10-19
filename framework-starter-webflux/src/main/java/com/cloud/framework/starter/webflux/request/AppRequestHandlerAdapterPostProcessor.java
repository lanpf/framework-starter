package com.cloud.framework.starter.webflux.request;

import java.util.List;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;

/** Decorates only Controller readers, before the adapter builds its standard argument resolvers. */
public final class AppRequestHandlerAdapterPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RequestMappingHandlerAdapter adapter) {
            List<HttpMessageReader<?>> readers = adapter.getMessageReaders().stream()
                    .<HttpMessageReader<?>>map(AppRequestHandlerAdapterPostProcessor::decorate)
                    .toList();
            adapter.setMessageReaders(readers);
        }
        return bean;
    }

    private static HttpMessageReader<?> decorate(HttpMessageReader<?> reader) {
        return reader instanceof AppRequestMessageReader<?> ? reader : new AppRequestMessageReader<>(reader);
    }
}
