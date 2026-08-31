package com.cloud.framework.starter.webmvc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloud.framework.core.AuthenticatedSessionChannelClientRequest;
import com.cloud.framework.core.AuthenticatedSessionClientRequest;
import com.cloud.framework.core.ChannelClientRequest;
import com.cloud.framework.core.ClientRequest;
import com.cloud.framework.core.RequestHeader;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;

class ClientRequestArgumentResolverTest {

    private final ClientRequestArgumentResolver resolver = new ClientRequestArgumentResolver();
    private final WebDataBinderFactory binderFactory = createBinderFactory();

    @Test
    void shouldResolveDirectClientChannelRequestFromHeaders() throws Exception {
        MethodParameter parameter = parameter("channelContext", ChannelClientRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.CLIENT_APP_ID, "admin-console");
        servletRequest.addHeader(RequestHeader.CLIENT_PLATFORM, "WEB");
        servletRequest.addHeader(RequestHeader.CLIENT_VERSION, "1.0");
        servletRequest.addHeader(RequestHeader.CHANNEL_CODE, "PARTNER_A");

        ChannelClientRequest request = (ChannelClientRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("admin-console", request.getClientAppId());
        assertEquals("WEB", request.getClientPlatform());
        assertEquals("1.0", request.getClientVersion());
        assertEquals("PARTNER_A", request.getChannelCode());
    }

    @Test
    void shouldOnlySupportDirectNonBodyContextTypes() throws Exception {
        assertTrue(resolver.supportsParameter(parameter("clientContext", ClientRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("channelContext", ChannelClientRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("authenticatedContext", AuthenticatedSessionClientRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("authenticatedChannelContext", AuthenticatedSessionChannelClientRequest.class)));
        assertFalse(resolver.supportsParameter(parameter("requestBody", ClientRequest.class)));
        assertFalse(resolver.supportsParameter(parameter("specialized", SpecializedRequest.class)));
    }

    @Test
    void shouldResolveAuthenticatedRequestFromHeaders() throws Exception {
        MethodParameter parameter = parameter("authenticatedContext", AuthenticatedSessionClientRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.CLIENT_APP_ID, "admin-console");
        servletRequest.addHeader(RequestHeader.USER_ID, "1001");
        servletRequest.addHeader(RequestHeader.SESSION_ID, "session-abc");

        AuthenticatedSessionClientRequest request = (AuthenticatedSessionClientRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("admin-console", request.getClientAppId());
        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    @Test
    void shouldComposeChannelAndAuthenticatedSessionContexts() throws Exception {
        MethodParameter parameter = parameter("authenticatedChannelContext", AuthenticatedSessionChannelClientRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.CLIENT_APP_ID, "mini-program");
        servletRequest.addHeader(RequestHeader.CHANNEL_CODE, "PARTNER_A");
        servletRequest.addHeader(RequestHeader.USER_ID, "1001");
        servletRequest.addHeader(RequestHeader.SESSION_ID, "session-abc");

        AuthenticatedSessionChannelClientRequest request = (AuthenticatedSessionChannelClientRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("mini-program", request.getClientAppId());
        assertEquals("PARTNER_A", request.getChannelCode());
        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    @Test
    void shouldRejectClientRequestWithoutClientAppId() throws Exception {
        MethodParameter parameter = parameter("clientContext", ClientRequest.class);

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter,
                        null,
                        new ServletWebRequest(new MockHttpServletRequest()),
                        binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("clientAppId"));
    }

    @Test
    void shouldRejectChannelClientRequestWithoutChannelCode() throws Exception {
        MethodParameter parameter = parameter("channelContext", ChannelClientRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.CLIENT_APP_ID, "admin-console");

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter, null, new ServletWebRequest(servletRequest), binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("channelCode"));
    }

    @Test
    void shouldAcceptClientRequestWithoutChannelCode() throws Exception {
        MethodParameter parameter = parameter("clientContext", ClientRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.CLIENT_APP_ID, "public-client");

        ClientRequest request = (ClientRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("public-client", request.getClientAppId());
    }

    @Test
    void shouldNotValidateWithoutValidationAnnotation() throws Exception {
        MethodParameter parameter = parameter("unvalidatedClientContext", ClientRequest.class);

        ClientRequest request = (ClientRequest) resolver.resolveArgument(
                parameter,
                null,
                new ServletWebRequest(new MockHttpServletRequest()),
                binderFactory);

        assertNull(request.getClientAppId());
    }

    @Test
    void shouldSupportValidatedAnnotation() throws Exception {
        MethodParameter parameter = parameter("validatedClientContext", ClientRequest.class);

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter,
                        null,
                        new ServletWebRequest(new MockHttpServletRequest()),
                        binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("clientAppId"));
    }

    private static MethodParameter parameter(String methodName, Class<?> parameterType) throws Exception {
        Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static WebDataBinderFactory createBinderFactory() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
        initializer.setValidator(validator);
        return new DefaultDataBinderFactory(initializer);
    }

    @SuppressWarnings("unused")
    private static final class SampleController {
        void clientContext(@Valid ClientRequest request) {
        }

        void channelContext(@Valid ChannelClientRequest request) {
        }

        void authenticatedContext(@Valid AuthenticatedSessionClientRequest request) {
        }

        void authenticatedChannelContext(@Valid AuthenticatedSessionChannelClientRequest request) {
        }

        void unvalidatedClientContext(ClientRequest request) {
        }

        void validatedClientContext(@Validated ClientRequest request) {
        }

        void requestBody(@RequestBody ClientRequest request) {
        }

        void specialized(SpecializedRequest request) {
        }
    }

    private static final class SpecializedRequest extends ClientRequest {
    }
}
