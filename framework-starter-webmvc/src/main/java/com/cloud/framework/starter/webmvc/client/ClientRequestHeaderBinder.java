package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.AuthenticatedSessionContext;
import com.cloud.framework.core.ChannelContext;
import com.cloud.framework.core.ClientRequest;
import com.cloud.framework.core.RequestHeader;
import java.util.function.Function;

/** 将受保护请求 Header 统一绑定到客户端上下文请求。 */
final class ClientRequestHeaderBinder {

    private ClientRequestHeaderBinder() {
    }

    static void bind(ClientRequest request, Function<String, String> headerValueProvider) {
        request.setClientAppId(headerValueProvider.apply(RequestHeader.CLIENT_APP_ID));
        request.setClientPlatform(headerValueProvider.apply(RequestHeader.CLIENT_PLATFORM));
        request.setClientVersion(headerValueProvider.apply(RequestHeader.CLIENT_VERSION));
        if (request instanceof ChannelContext channelContext) {
            channelContext.setChannelCode(headerValueProvider.apply(RequestHeader.CHANNEL_CODE));
        }
        if (request instanceof AuthenticatedSessionContext sessionContext) {
            sessionContext.setUserId(headerValueProvider.apply(RequestHeader.USER_ID));
            sessionContext.setSessionId(headerValueProvider.apply(RequestHeader.SESSION_ID));
        }
    }
}
