package com.cloud.framework.starter.webflux.error;

import com.cloud.framework.core.request.RequestError;
import com.cloud.framework.kernel.error.BaseException;
import com.cloud.framework.kernel.error.Error;
import com.cloud.framework.kernel.error.FrameworkError;
import com.cloud.framework.kernel.error.FrameworkException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class WebFluxExceptionHandlerTest {
    private final WebFluxExceptionHandler handler = new WebFluxExceptionHandler();

    @Test
    void shouldTranslateValidationWithoutLeakingRejectedValues() {
        var response = handler.validationFailure(new ConstraintViolationException("sensitive rejected value", Set.of()));
        assertEquals(400, response.getStatusCode().value());
        assertEquals(RequestError.REQUEST_INVALID.errorCode(), response.getBody().code());
        assertEquals(RequestError.REQUEST_INVALID.errorMessage(), response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldPreserveBusinessErrorCodesAndHttpStatuses() {
        var exception = new TestBusinessException();
        assertEquals(exception.getErrorCode(), handler.failure(exception).getBody().code());
        var response = handler.httpFailure(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "internal detail"));
        assertEquals(415, response.getStatusCode().value());
        assertEquals(RequestError.REQUEST_REJECTED.errorMessage(), response.getBody().message());
    }

    @Test
    void shouldReturnStableFrameworkFailureWithoutDiagnosticDetails() {
        var response = handler.failure(FrameworkException.illegalState("internal detail"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals(FrameworkError.ILLEGAL_STATE.errorCode(), response.getBody().code());
        assertEquals(FrameworkError.ILLEGAL_STATE.errorMessage(), response.getBody().message());
    }

    @Test
    void shouldHideUnexpectedFailureDetails() {
        var response = handler.failure(new IllegalStateException("internal failure"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals(FrameworkError.INTERNAL_ERROR.errorCode(), response.getBody().code());
        assertEquals(FrameworkError.INTERNAL_ERROR.errorMessage(), response.getBody().message());
    }

    @Test
    void shouldTreatInvalidReturnValuesAsServerFailures() throws Exception {
        try (var factory = jakarta.validation.Validation.byDefaultProvider().configure()
                .messageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator())
                .buildValidatorFactory()) {
            var violations = factory.getValidator().forExecutables().validateReturnValue(
                    new InvalidResponse(), InvalidResponse.class.getMethod("value"), null);
            var response = handler.validationFailure(new ConstraintViolationException(violations));
            assertEquals(500, response.getStatusCode().value());
            assertEquals(FrameworkError.INTERNAL_ERROR.errorCode(), response.getBody().code());
        }
    }

    public static class InvalidResponse {
        @jakarta.validation.constraints.NotNull
        public String value() { return null; }
    }

    enum TestBusinessError implements Error {
        REJECTED;

        @Override
        public String getErrorCode() { return "200001"; }

        @Override
        public String getErrorMessage() { return "业务拒绝"; }
    }

    static final class TestBusinessException extends BaseException {
        private TestBusinessException() { super(TestBusinessError.REJECTED); }
    }
}
