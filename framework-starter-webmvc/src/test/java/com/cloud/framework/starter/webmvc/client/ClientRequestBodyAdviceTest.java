package com.cloud.framework.starter.webmvc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cloud.framework.core.AuthenticatedRequest;
import com.cloud.framework.core.ClientChannelRequest;
import com.cloud.framework.core.ClientRequest;
import com.cloud.framework.core.RequestHeader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.MockHttpInputMessage;

class ClientRequestBodyAdviceTest {

    @Test
    void shouldOverwriteClientRequestContextFromHeaders() {
        ClientRequestBodyAdvice advice = new ClientRequestBodyAdvice();
        SampleClientChannelRequest request = new SampleClientChannelRequest();
        request.setClientAppId("body-app");
        request.setClientPlatform("body-platform");
        request.setClientVersion("body-version");
        request.setChannelCode("body-channel");
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        HttpHeaders headers = inputMessage.getHeaders();
        headers.add(RequestHeader.CLIENT_APP_ID, "gateway-app");
        headers.add(RequestHeader.CLIENT_PLATFORM, "SERVICE");
        headers.add(RequestHeader.CLIENT_VERSION, "1.0");
        headers.add(RequestHeader.CHANNEL_CODE, "PARTNER_A");

        advice.afterBodyRead(request, inputMessage, null, SampleClientChannelRequest.class, null);

        assertEquals("gateway-app", request.getClientAppId());
        assertEquals("SERVICE", request.getClientPlatform());
        assertEquals("1.0", request.getClientVersion());
        assertEquals("PARTNER_A", request.getChannelCode());
    }

    @Test
    void shouldOnlyBindContextAndLeaveMissingHeaderValidationToSpringMvc() {
        ClientRequestBodyAdvice advice = new ClientRequestBodyAdvice();
        SampleClientChannelRequest request = new SampleClientChannelRequest();
        request.setClientAppId("untrusted-body-value");

        advice.afterBodyRead(request,
                new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8)),
                null, SampleClientChannelRequest.class, null);

        assertNull(request.getClientAppId());
    }

    @Test
    void shouldPopulateDirectClientRequestInstance() {
        ClientRequest request = new ClientRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.CLIENT_APP_ID, "gateway-app");

        new ClientRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, ClientRequest.class, null);

        assertEquals("gateway-app", request.getClientAppId());
    }

    @Test
    void shouldPopulateDirectClientChannelRequestInstance() {
        ClientChannelRequest request = new ClientChannelRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.CHANNEL_CODE, "PARTNER_A");

        new ClientRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, ClientChannelRequest.class, null);

        assertEquals("PARTNER_A", request.getChannelCode());
    }

    @Test
    void shouldPopulateAuthenticatedRequestInstance() {
        AuthenticatedRequest request = new AuthenticatedRequest();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        inputMessage.getHeaders().add(RequestHeader.USER_ID, "1001");

        new ClientRequestBodyAdvice().afterBodyRead(
                request, inputMessage, null, AuthenticatedRequest.class, null);

        assertEquals(1001L, request.getUserId());
    }

    private static final class SampleClientChannelRequest extends ClientChannelRequest {
    }
}
