package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionAppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionClientAppRequest;
import com.cloud.framework.core.request.ClientAppRequest;
import com.cloud.framework.core.http.RequestHeader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppRequestBodyAdviceTest {
    private static final String CLIENT_ID_HEADER = RequestHeader.CLIENT_ID.headerName();

    @Test
    void shouldOverwriteClientRequestContextFromHeaders() {
        AppRequestBodyAdvice advice = new AppRequestBodyAdvice();
        SampleClientAppRequest request = new SampleClientAppRequest();
        request.setAppId("body-app");
        request.setAppPlatform("body-platform");
        request.setAppVersion("body-version");
        request.setClientId("body-channel");
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        HttpHeaders headers = inputMessage.getHeaders();
        headers.add(RequestHeader.APP_ID.headerName(), "gateway-app");
        headers.add(RequestHeader.APP_PLATFORM.headerName(), "SERVICE");
        headers.add(RequestHeader.APP_VERSION.headerName(), "1.0");
        headers.add(CLIENT_ID_HEADER, "PARTNER_A");

        advice.afterBodyRead(request, inputMessage, null, SampleClientAppRequest.class, null);

        assertEquals("gateway-app", request.getAppId());
        assertEquals("SERVICE", request.getAppPlatform());
        assertEquals("1.0", request.getAppVersion());
        assertEquals("PARTNER_A", request.getClientId());
    }

    @Test
    void shouldOnlyBindContextAndLeaveMissingHeaderValidationToSpringMvc() {
        AppRequestBodyAdvice advice = new AppRequestBodyAdvice();
        SampleClientAppRequest request = new SampleClientAppRequest();
        request.setAppId("untrusted-body-value");

        advice.afterBodyRead(request,
                new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8)),
                null, SampleClientAppRequest.class, null);

        assertNull(request.getAppId());
    }

    @Test
    void shouldPopulateDirectClientRequestInstance() {
        AppRequest request = new AppRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.APP_ID.headerName(), "gateway-app");

        new AppRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, AppRequest.class, null);

        assertEquals("gateway-app", request.getAppId());
    }

    @Test
    void shouldPopulateDirectClientAppRequestInstance() {
        ClientAppRequest request = new ClientAppRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(CLIENT_ID_HEADER, "PARTNER_A");

        new AppRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, ClientAppRequest.class, null);

        assertEquals("PARTNER_A", request.getClientId());
    }

    @Test
    void shouldPopulateAuthenticatedRequestInstance() {
        AuthenticatedSessionAppRequest request = new AuthenticatedSessionAppRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.USER_ID.headerName(), "1001");
        inputMessage.getHeaders().add(RequestHeader.SESSION_ID.headerName(), "session-abc");

        new AppRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, AuthenticatedSessionAppRequest.class, null);

        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    @Test
    void shouldPopulateComposedChannelAndAuthenticatedSessionContexts() {
        AuthenticatedSessionClientAppRequest request = new AuthenticatedSessionClientAppRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.APP_ID.headerName(), "mini-program");
        inputMessage.getHeaders().add(CLIENT_ID_HEADER, "PARTNER_A");
        inputMessage.getHeaders().add(RequestHeader.USER_ID.headerName(), "1001");
        inputMessage.getHeaders().add(RequestHeader.SESSION_ID.headerName(), "session-abc");

        new AppRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, AuthenticatedSessionClientAppRequest.class, null);

        assertEquals("mini-program", request.getAppId());
        assertEquals("PARTNER_A", request.getClientId());
        assertEquals("1001", request.getUserId());
        assertEquals("session-abc", request.getSessionId());
    }

    private static final class SampleClientAppRequest extends ClientAppRequest {
    }
}
