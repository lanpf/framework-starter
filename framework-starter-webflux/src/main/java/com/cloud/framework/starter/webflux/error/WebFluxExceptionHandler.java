package com.cloud.framework.starter.webflux.error;

import com.cloud.framework.core.request.RequestError;
import com.cloud.framework.kernel.error.BaseError;
import com.cloud.framework.kernel.error.BaseException;
import com.cloud.framework.kernel.error.FrameworkError;
import com.cloud.framework.kernel.error.FrameworkException;
import com.cloud.framework.core.response.Result;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Controller exception translation; ingress filters and proxied responses keep their own boundaries. */
@Slf4j
@RestControllerAdvice
public class WebFluxExceptionHandler {
    @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, MethodValidationException.class})
    public ResponseEntity<Result<Void>> validationFailure(Exception exception) {
        boolean returnValue = exception instanceof MethodValidationException validation && validation.isForReturnValue()
                || exception instanceof HandlerMethodValidationException handlerValidation && handlerValidation.isForReturnValue()
                || exception instanceof ConstraintViolationException constraintValidation && constraintValidation.getConstraintViolations().stream()
                    .anyMatch(violation -> {
                        for (Path.Node node : violation.getPropertyPath()) {
                            if (node.getKind() == ElementKind.RETURN_VALUE) { return true; }
                        }
                        return false;
                    });
        if (returnValue) {
            return ResponseEntity.internalServerError().body(Result.failure(FrameworkError.INTERNAL_ERROR));
        }
        return ResponseEntity.badRequest().body(Result.failure(
                new FrameworkException(RequestError.REQUEST_INVALID, exception)));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Result<Void>> httpFailure(ErrorResponseException exception) {
        BaseError error = exception.getStatusCode().is4xxClientError()
                ? RequestError.REQUEST_REJECTED : FrameworkError.INTERNAL_ERROR;
        return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders()).body(Result.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> failure(Exception exception) {
        if (exception instanceof FrameworkException framework) {
            log.error("Framework controller exception.", exception);
            return ResponseEntity.internalServerError().body(Result.failure(framework.getError()));
        }
        if (exception instanceof BaseException base) {
            return ResponseEntity.ok(Result.failure(base));
        }
        log.error("Unhandled controller exception.", exception);
        return ResponseEntity.internalServerError().body(Result.failure(FrameworkError.INTERNAL_ERROR));
    }
}
