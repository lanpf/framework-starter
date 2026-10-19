package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionAppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionClientAppRequest;
import com.cloud.framework.core.request.ClientAppRequest;
import com.cloud.framework.core.http.RequestHeader;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppRequestArgumentResolverTest {
    private static final String CLIENT_ID_HEADER = RequestHeader.CLIENT_ID.headerName();

    private final AppRequestArgumentResolver resolver = new AppRequestArgumentResolver();
    private final WebDataBinderFactory binderFactory = createBinderFactory();

    @Test
    void shouldResolveDirectClientChannelRequestFromHeaders() throws Exception {
        MethodParameter parameter = parameter("clientContextValue", ClientAppRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.APP_ID.headerName(), "admin-console");
        servletRequest.addHeader(RequestHeader.APP_PLATFORM.headerName(), "WEB");
        servletRequest.addHeader(RequestHeader.APP_VERSION.headerName(), "1.0");
        servletRequest.addHeader(CLIENT_ID_HEADER, "PARTNER_A");

        ClientAppRequest request = (ClientAppRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("admin-console", request.getAppId());
        assertEquals("WEB", request.getAppPlatform());
        assertEquals("1.0", request.getAppVersion());
        assertEquals("PARTNER_A", request.getClientId());
    }

    @Test
    void shouldOnlySupportDirectNonBodyContextTypes() throws Exception {
        assertTrue(resolver.supportsParameter(parameter("clientContext", AppRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("clientContextValue", ClientAppRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("authenticatedContext", AuthenticatedSessionAppRequest.class)));
        assertTrue(resolver.supportsParameter(parameter("authenticatedClientContext", AuthenticatedSessionClientAppRequest.class)));
        assertFalse(resolver.supportsParameter(parameter("requestBody", AppRequest.class)));
        assertFalse(resolver.supportsParameter(parameter("specialized", SpecializedRequest.class)));
    }

    @Test
    void shouldResolveAuthenticatedRequestFromHeaders() throws Exception {
        MethodParameter parameter = parameter("authenticatedContext", AuthenticatedSessionAppRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.APP_ID.headerName(), "admin-console");
        servletRequest.addHeader(RequestHeader.USER_ID.headerName(), "1001");
        servletRequest.addHeader(RequestHeader.SESSION_ID.headerName(), "session-abc");

        AuthenticatedSessionAppRequest request = (AuthenticatedSessionAppRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("admin-console", request.getAppId());
        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    @Test
    void shouldComposeChannelAndAuthenticatedSessionContexts() throws Exception {
        MethodParameter parameter = parameter("authenticatedClientContext", AuthenticatedSessionClientAppRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.APP_ID.headerName(), "mini-program");
        servletRequest.addHeader(CLIENT_ID_HEADER, "PARTNER_A");
        servletRequest.addHeader(RequestHeader.USER_ID.headerName(), "1001");
        servletRequest.addHeader(RequestHeader.SESSION_ID.headerName(), "session-abc");

        AuthenticatedSessionClientAppRequest request = (AuthenticatedSessionClientAppRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("mini-program", request.getAppId());
        assertEquals("PARTNER_A", request.getClientId());
        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    @Test
    void shouldRejectClientRequestWithoutAppId() throws Exception {
        MethodParameter parameter = parameter("clientContext", AppRequest.class);

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter,
                        null,
                        new ServletWebRequest(new MockHttpServletRequest()),
                        binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("appId"));
    }

    @Test
    void shouldRejectClientAppRequestWithoutClientId() throws Exception {
        MethodParameter parameter = parameter("clientContextValue", ClientAppRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.APP_ID.headerName(), "admin-console");

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter, null, new ServletWebRequest(servletRequest), binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("clientId"));
    }

    @Test
    void shouldAcceptClientRequestWithoutClientId() throws Exception {
        MethodParameter parameter = parameter("clientContext", AppRequest.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(RequestHeader.APP_ID.headerName(), "public-client");

        AppRequest request = (AppRequest) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(servletRequest), binderFactory);

        assertEquals("public-client", request.getAppId());
    }

    @Test
    void shouldNotValidateWithoutValidationAnnotation() throws Exception {
        MethodParameter parameter = parameter("unvalidatedClientContext", AppRequest.class);

        AppRequest request = (AppRequest) resolver.resolveArgument(
                parameter,
                null,
                new ServletWebRequest(new MockHttpServletRequest()),
                binderFactory);

        assertNull(request.getAppId());
    }

    @Test
    void shouldSupportValidatedAnnotation() throws Exception {
        MethodParameter parameter = parameter("validatedClientContext", AppRequest.class);

        MethodArgumentNotValidException exception = assertThrows(
                MethodArgumentNotValidException.class,
                () -> resolver.resolveArgument(
                        parameter,
                        null,
                        new ServletWebRequest(new MockHttpServletRequest()),
                        binderFactory));

        assertTrue(exception.getBindingResult().hasFieldErrors("appId"));
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
        void clientContext(@Valid AppRequest request) {
        }

        void clientContextValue(@Valid ClientAppRequest request) {
        }

        void authenticatedContext(@Valid AuthenticatedSessionAppRequest request) {
        }

        void authenticatedClientContext(@Valid AuthenticatedSessionClientAppRequest request) {
        }

        void unvalidatedClientContext(AppRequest request) {
        }

        void validatedClientContext(@Validated AppRequest request) {
        }

        void requestBody(@RequestBody AppRequest request) {
        }

        void specialized(SpecializedRequest request) {
        }
    }

    private static final class SpecializedRequest extends AppRequest {
    }
}
