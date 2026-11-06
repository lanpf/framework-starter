package com.cloud.framework.starter.webmvc.error;

import com.cloud.framework.core.request.RequestError;
import com.cloud.framework.core.response.Result;
import com.cloud.framework.kernel.error.BaseError;
import com.cloud.framework.kernel.error.BaseException;
import com.cloud.framework.kernel.error.FrameworkError;
import com.cloud.framework.kernel.error.FrameworkException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Controller exception translation; Servlet filters and security retain their own boundaries. */
@Slf4j
@RestControllerAdvice
public class WebMvcExceptionHandler {

    @ExceptionHandler({BindException.class, ConstraintViolationException.class,
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

    @ExceptionHandler({ErrorResponseException.class, HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Result<Void>> httpFailure(Exception exception) {
        if (exception instanceof ErrorResponse response) {
            BaseError error = response.getStatusCode().is4xxClientError()
                    ? RequestError.REQUEST_REJECTED : FrameworkError.INTERNAL_ERROR;
            return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(Result.failure(error));
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return ResponseEntity.badRequest().body(Result.failure(RequestError.REQUEST_MALFORMED));
        }
        return ResponseEntity.badRequest().body(Result.failure(RequestError.REQUEST_INVALID));
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
        // Several Servlet protocol exceptions implement ErrorResponse directly rather than extending ErrorResponseException.
        if (exception instanceof ErrorResponse) {
            return httpFailure(exception);
        }
        log.error("Unhandled controller exception.", exception);
        return ResponseEntity.internalServerError().body(Result.failure(FrameworkError.INTERNAL_ERROR));
    }
}
