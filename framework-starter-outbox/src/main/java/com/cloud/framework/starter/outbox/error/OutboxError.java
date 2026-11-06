package com.cloud.framework.starter.outbox.error;

import com.cloud.framework.kernel.error.BaseError;
import com.cloud.framework.kernel.error.ErrorCodeNamespace;
import com.cloud.framework.kernel.error.ErrorCodePrefix;
import com.cloud.framework.kernel.error.ErrorCodeRange;

public enum OutboxError implements BaseError {
    SERIALIZATION_FAILED(500, "Outbox event serialization failed"),
    DESERIALIZATION_FAILED(501, "Outbox event deserialization failed"),
    BATCH_AGGREGATE_MISMATCH(502, "Outbox batch contains multiple aggregates"),
    STATE_UPDATE_FAILED(503, "Outbox state update failed");

    private static final ErrorCodePrefix PREFIX = new ErrorCodePrefix(ErrorCodeNamespace.FRAMEWORK, 0);

    private final int localCode;
    private final String message;

    OutboxError(int localCode, String message) {
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
