package com.cloud.framework.starter.webflux.request;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionContext;
import com.cloud.framework.core.request.ClientContext;
import com.cloud.framework.core.http.RequestHeader;
import java.util.function.Function;

/** Consumes headers established by a trusted ingress; absent headers also replace body declarations. */
final class AppRequestHeaderBinder {
    private AppRequestHeaderBinder() { }

    static void bind(Object value, Function<String, String> headers) {
        if (value instanceof AppRequest request) {
            request.setAppId(headers.apply(RequestHeader.APP_ID.headerName()));
            request.setAppPlatform(headers.apply(RequestHeader.APP_PLATFORM.headerName()));
            request.setAppVersion(headers.apply(RequestHeader.APP_VERSION.headerName()));
            if (request instanceof ClientContext client) {
                client.setClientId(headers.apply(RequestHeader.CLIENT_ID.headerName()));
            }
            if (request instanceof AuthenticatedSessionContext session) {
                session.setUserId(headers.apply(RequestHeader.USER_ID.headerName()));
                session.setSessionId(headers.apply(RequestHeader.SESSION_ID.headerName()));
            }
        }
    }
}
