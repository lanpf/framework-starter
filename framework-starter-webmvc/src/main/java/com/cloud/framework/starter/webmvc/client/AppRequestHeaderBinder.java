package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionContext;
import com.cloud.framework.core.request.ClientContext;
import com.cloud.framework.core.http.RequestHeader;
import java.util.function.Function;

/** 将受保护请求 Header 统一绑定到客户端上下文请求。 */
final class AppRequestHeaderBinder {

    private AppRequestHeaderBinder() {
    }

    static void bind(AppRequest request, Function<String, String> headerValueProvider) {
        request.setAppId(headerValueProvider.apply(RequestHeader.APP_ID.headerName()));
        request.setAppPlatform(headerValueProvider.apply(RequestHeader.APP_PLATFORM.headerName()));
        request.setAppVersion(headerValueProvider.apply(RequestHeader.APP_VERSION.headerName()));
        if (request instanceof ClientContext clientContextValue) {
            clientContextValue.setClientId(headerValueProvider.apply(RequestHeader.CLIENT_ID.headerName()));
        }
        if (request instanceof AuthenticatedSessionContext sessionContext) {
            sessionContext.setUserId(headerValueProvider.apply(RequestHeader.USER_ID.headerName()));
            sessionContext.setSessionId(headerValueProvider.apply(RequestHeader.SESSION_ID.headerName()));
        }
    }
}
