package com.cloud.framework.starter.webmvc.error;

import com.cloud.framework.core.BaseResult;
import com.cloud.framework.core.Result;
import com.cloud.framework.core.error.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public BaseResult handle(Exception exception) {
        if (exception instanceof BaseException baseException) {
            return Result.failure(baseException);
        }
        log.error("Unhandled exception.", exception);
        return Result.failure("-1", exception.getMessage());
    }
}
