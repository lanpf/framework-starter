package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.ClientRequest;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * 将入口层验证后的客户端上下文写入 {@link ClientRequest}。
 *
 * <p>这里只完成 Header 绑定；返回后由 Spring MVC 根据方法参数上的
 * {@code @Valid}/{@code @Validated} 执行请求体 Bean Validation。</p>
 */
@ControllerAdvice
public class ClientRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return ClientRequest.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        ClientRequest request = (ClientRequest) body;
        ClientRequestHeaderBinder.bind(request, inputMessage.getHeaders()::getFirst);
        return request;
    }

}
