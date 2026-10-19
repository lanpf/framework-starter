package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionAppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionClientAppRequest;
import com.cloud.framework.core.request.ClientAppRequest;
import java.lang.annotation.Annotation;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 从受保护 Header 创建无请求体接口显式声明的客户端上下文参数。 */
public class AppRequestArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return false;
        }
        Class<?> parameterType = parameter.getParameterType();
        return parameterType == AppRequest.class
                || parameterType == ClientAppRequest.class
                || parameterType == AuthenticatedSessionAppRequest.class
                || parameterType == AuthenticatedSessionClientAppRequest.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        AppRequest request = createRequest(parameter.getParameterType());
        AppRequestHeaderBinder.bind(request, webRequest::getHeader);
        validateIfApplicable(parameter, webRequest, binderFactory, request);
        return request;
    }

    private void validateIfApplicable(
            MethodParameter parameter,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory,
            AppRequest request
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
            AppRequest request,
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

    private AppRequest createRequest(Class<?> parameterType) {
        if (parameterType == ClientAppRequest.class) {
            return new ClientAppRequest();
        }
        if (parameterType == AuthenticatedSessionAppRequest.class) {
            return new AuthenticatedSessionAppRequest();
        }
        if (parameterType == AuthenticatedSessionClientAppRequest.class) {
            return new AuthenticatedSessionClientAppRequest();
        }
        return new AppRequest();
    }
}
