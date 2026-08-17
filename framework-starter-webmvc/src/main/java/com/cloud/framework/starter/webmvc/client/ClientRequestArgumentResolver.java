package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.AuthenticatedRequest;
import com.cloud.framework.core.ClientChannelRequest;
import com.cloud.framework.core.ClientRequest;
import java.lang.annotation.Annotation;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 从受保护 Header 创建无请求体接口显式声明的客户端上下文参数。 */
public class ClientRequestArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return false;
        }
        Class<?> parameterType = parameter.getParameterType();
        return parameterType == ClientRequest.class
                || parameterType == ClientChannelRequest.class
                || parameterType == AuthenticatedRequest.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        ClientRequest request = createRequest(parameter.getParameterType());
        ClientRequestHeaderBinder.bind(request, webRequest::getHeader);
        validateIfApplicable(parameter, webRequest, binderFactory, request);
        return request;
    }

    private void validateIfApplicable(
            MethodParameter parameter,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory,
            ClientRequest request
    ) throws Exception {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(annotation);
            if (validationHints != null) {
                validate(parameter, webRequest, binderFactory, request, validationHints);
                return;
            }
        }
    }

    private void validate(
            MethodParameter parameter,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory,
            ClientRequest request,
            Object[] validationHints
    ) throws Exception {
        WebDataBinder binder = binderFactory.createBinder(
                webRequest, request, Conventions.getVariableNameForParameter(parameter));
        binder.validate(validationHints);
        BindingResult bindingResult = binder.getBindingResult();
        if (bindingResult.hasErrors()) {
            throw new MethodArgumentNotValidException(parameter, bindingResult);
        }
    }

    private ClientRequest createRequest(Class<?> parameterType) {
        if (parameterType == ClientChannelRequest.class) {
            return new ClientChannelRequest();
        }
        if (parameterType == AuthenticatedRequest.class) {
            return new AuthenticatedRequest();
        }
        return new ClientRequest();
    }
}
