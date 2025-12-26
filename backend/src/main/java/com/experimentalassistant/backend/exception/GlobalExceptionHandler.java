package com.experimentalassistant.backend.exception;

import com.experimentalassistant.backend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Business Logic Error (Runtime Exception) -> User Readable
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK) // We use 200 with code != 0 for business errors
    public Result<String> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.warn("Business Error: URI={} Error={}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getMessage()); // Assuming message is safe for user
    }

    // 2. Validation Error -> User Error
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleValidationException(Exception e) {
        log.warn("Validation Error: {}", e.getMessage());
        return Result.error("Form validation failed. Please check your inputs.");
    }

    // 3. System Error (SQL, NPE, etc) -> "Server Busy"
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(Exception e, HttpServletRequest request) {
        log.error("System Error: URI={} Error={}", request.getRequestURI(), e.getMessage(), e);
        // Hide stack trace from response
        return Result.error("System encountered an unexpected error. Please try again later.");
    }
}
