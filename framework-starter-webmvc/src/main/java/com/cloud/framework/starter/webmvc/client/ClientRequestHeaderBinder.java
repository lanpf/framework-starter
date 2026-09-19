package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.AuthenticatedSessionContext;
import com.cloud.framework.core.ChannelContext;
import com.cloud.framework.core.ClientRequest;
import com.cloud.framework.core.RequestHeader;
import java.util.function.Function;

/** 将受保护请求 Header 统一绑定到客户端上下文请求。 */
final class ClientRequestHeaderBinder {
    private static final String CHANNEL_CODE_HEADER = "X-Channel-Code";

    private ClientRequestHeaderBinder() {
    }

    static void bind(ClientRequest request, Function<String, String> headerValueProvider) {
        request.setClientAppId(headerValueProvider.apply(RequestHeader.APP_ID.headerName()));
        request.setClientPlatform(headerValueProvider.apply(RequestHeader.APP_PLATFORM.headerName()));
        request.setClientVersion(headerValueProvider.apply(RequestHeader.APP_VERSION.headerName()));
        if (request instanceof ChannelContext channelContext) {
            channelContext.setChannelCode(headerValueProvider.apply(CHANNEL_CODE_HEADER));
        }
        if (request instanceof AuthenticatedSessionContext sessionContext) {
            sessionContext.setUserId(headerValueProvider.apply(RequestHeader.USER_ID.headerName()));
            sessionContext.setSessionId(headerValueProvider.apply(RequestHeader.SESSION_ID.headerName()));
        }
    }
}
