package com.cloud.framework.starter.domain.eventstore.error;

import com.cloud.framework.kernel.error.BaseError;
import com.cloud.framework.kernel.error.ErrorCodeNamespace;
import com.cloud.framework.kernel.error.ErrorCodePrefix;
import com.cloud.framework.kernel.error.ErrorCodeRange;

public enum DomainEventStoreError implements BaseError {
    SERIALIZATION_FAILED(520, "Domain event serialization failed");

    private static final ErrorCodePrefix PREFIX = new ErrorCodePrefix(ErrorCodeNamespace.FRAMEWORK, 0);

    private final int localCode;
    private final String message;

    DomainEventStoreError(int localCode, String message) {
        ErrorCodeRange.assertLocalCode(localCode);
        this.localCode = localCode;
        this.message = message;
    }

    @Override
    public ErrorCodePrefix getPrefix() {
        return PREFIX;
    }

    @Override
    public int getLocalCode() {
        return localCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
