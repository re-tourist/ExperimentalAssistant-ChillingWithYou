package com.experimentalassistant.backend.exception;

import com.experimentalassistant.backend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MyBatisSystemException.class, PersistenceException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<String> handleMyBatisException(RuntimeException e, HttpServletRequest request) {
        if (hasCause(e, CannotGetJdbcConnectionException.class)) {
            log.warn("Database Connection Error: URI={} Error={}", request.getRequestURI(), e.getMessage());
            return Result.error("数据库连接失败：请确认 MySQL 已启动，并在启动后端前设置环境变量 DB_USERNAME/DB_PASSWORD（或 MYSQL_USER/MYSQL_ROOT_PASSWORD）。");
        }

        SQLException sqlException = findCause(e, SQLException.class);
        if (sqlException != null) {
            String msg = sqlException.getMessage();
            if (msg != null && msg.contains("Access denied for user")) {
                log.warn("Database Auth Error: URI={} Error={}", request.getRequestURI(), msg);
                return Result.error("数据库账号或密码错误：请设置 DB_USERNAME/DB_PASSWORD（或 MYSQL_USER/MYSQL_ROOT_PASSWORD）后重试。");
            }
        }

        log.error("Database Error: URI={} Error={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("数据库访问失败，请稍后重试。");
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<String> handleCannotGetJdbcConnectionException(CannotGetJdbcConnectionException e, HttpServletRequest request) {
        log.warn("Database Connection Error: URI={} Error={}", request.getRequestURI(), e.getMessage());
        return Result.error("数据库连接失败：请确认 MySQL 已启动，并在启动后端前设置环境变量 DB_USERNAME/DB_PASSWORD（或 MYSQL_USER/MYSQL_ROOT_PASSWORD）。");
    }

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

    private static boolean hasCause(Throwable e, Class<? extends Throwable> causeType) {
        return findCause(e, causeType) != null;
    }

    private static <T extends Throwable> T findCause(Throwable e, Class<T> causeType) {
        Throwable cur = e;
        while (cur != null) {
            if (causeType.isInstance(cur)) {
                return causeType.cast(cur);
            }
            cur = cur.getCause();
        }
        return null;
    }
}
