package com.cloud.framework.starter.webflux.request;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionAppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionClientAppRequest;
import com.cloud.framework.core.request.ClientAppRequest;
import java.lang.annotation.Annotation;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Header-only binding for the four framework context types; request subclasses use RequestBody. */
public final class AppRequestArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        return !parameter.hasParameterAnnotation(RequestBody.class)
                && (type == AppRequest.class || type == ClientAppRequest.class
                    || type == AuthenticatedSessionAppRequest.class || type == AuthenticatedSessionClientAppRequest.class);
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            AppRequest request = create(parameter.getParameterType());
            AppRequestHeaderBinder.bind(request, exchange.getRequest().getHeaders()::getFirst);
            for (Annotation annotation : parameter.getParameterAnnotations()) {
                Object[] hints = ValidationAnnotationUtils.determineValidationHints(annotation);
                if (hints != null) {
                    WebExchangeDataBinder binder = context.createDataBinder(exchange, request,
                            Conventions.getVariableNameForParameter(parameter), ResolvableType.forMethodParameter(parameter));
                    binder.validate(hints);
                    if (binder.getBindingResult().hasErrors()) {
                        throw new WebExchangeBindException(parameter, binder.getBindingResult());
                    }
                    break;
                }
            }
            return request;
        });
    }

    private static AppRequest create(Class<?> type) {
        if (type == ClientAppRequest.class) { return new ClientAppRequest(); }
        if (type == AuthenticatedSessionAppRequest.class) { return new AuthenticatedSessionAppRequest(); }
        if (type == AuthenticatedSessionClientAppRequest.class) { return new AuthenticatedSessionClientAppRequest(); }
        return new AppRequest();
    }
}
