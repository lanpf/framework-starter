package com.cloud.framework.starter.webflux.request;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Binds each decoded request before Spring's standard RequestBody resolver performs validation. */
@RequiredArgsConstructor
final class AppRequestMessageReader<T> implements HttpMessageReader<T> {
    private final HttpMessageReader<T> delegate;

    @Override
    public List<MediaType> getReadableMediaTypes() { return delegate.getReadableMediaTypes(); }

    @Override
    public List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
        return delegate.getReadableMediaTypes(elementType);
    }

    @Override
    public boolean canRead(ResolvableType elementType, MediaType mediaType) {
        return delegate.canRead(elementType, mediaType);
    }

    @Override
    public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        return delegate.read(elementType, message, hints)
                .doOnNext(value -> AppRequestHeaderBinder.bind(value, message.getHeaders()::getFirst));
    }

    @Override
    public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        return delegate.readMono(elementType, message, hints)
                .doOnNext(value -> AppRequestHeaderBinder.bind(value, message.getHeaders()::getFirst));
    }

    @Override
    public Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
                        ServerHttpResponse response, Map<String, Object> hints) {
        return delegate.read(actualType, elementType, request, response, hints)
                .doOnNext(value -> AppRequestHeaderBinder.bind(value, request.getHeaders()::getFirst));
    }

    @Override
    public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
                            ServerHttpResponse response, Map<String, Object> hints) {
        return delegate.readMono(actualType, elementType, request, response, hints)
                .doOnNext(value -> AppRequestHeaderBinder.bind(value, request.getHeaders()::getFirst));
    }
}
