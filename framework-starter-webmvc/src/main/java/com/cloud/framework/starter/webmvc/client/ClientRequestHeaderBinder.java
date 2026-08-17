package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.AuthenticatedRequest;
import com.cloud.framework.core.ClientChannelRequest;
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
        if (request instanceof ClientChannelRequest channelRequest) {
            channelRequest.setChannelCode(headerValueProvider.apply(RequestHeader.CHANNEL_CODE));
        }
        if (request instanceof AuthenticatedRequest authenticatedRequest) {
            String userId = headerValueProvider.apply(RequestHeader.USER_ID);
            authenticatedRequest.setUserId(userId == null ? null : Long.valueOf(userId));
        }
    }
}
