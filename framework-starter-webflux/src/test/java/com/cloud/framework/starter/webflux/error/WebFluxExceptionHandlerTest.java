package com.cloud.framework.starter.webflux.error;

import com.cloud.framework.core.error.FrameworkError;
import com.cloud.framework.core.error.FrameworkException;
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
        assertEquals(FrameworkError.OBJECT_INVALID.errorCode(), response.getBody().code());
        assertEquals(FrameworkError.OBJECT_INVALID.errorMessage(), response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldPreserveBusinessErrorCodesAndHttpStatuses() {
        var exception = new FrameworkException(FrameworkError.OBJECT_INVALID);
        assertEquals(exception.getErrorCode(), handler.failure(exception).getBody().code());
        var response = handler.httpFailure(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "internal detail"));
        assertEquals(415, response.getStatusCode().value());
        assertEquals(FrameworkError.OBJECT_INVALID.errorMessage(), response.getBody().message());
    }

    @Test
    void shouldHideUnexpectedFailureDetails() {
        var response = handler.failure(new IllegalStateException("internal failure"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals(FrameworkError.FRAMEWORK_ERROR.errorCode(), response.getBody().code());
        assertEquals(FrameworkError.FRAMEWORK_ERROR.errorMessage(), response.getBody().message());
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
            assertEquals(FrameworkError.FRAMEWORK_ERROR.errorCode(), response.getBody().code());
        }
    }

    public static class InvalidResponse {
        @jakarta.validation.constraints.NotNull
        public String value() { return null; }
    }
}
