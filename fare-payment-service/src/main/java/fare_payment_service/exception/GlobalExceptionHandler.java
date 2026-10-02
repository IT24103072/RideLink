package fare_payment_service.exception;

import fare_payment_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API exception at {}: {} - {}", request.getRequestURI(), ex.getStatus(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(Instant.now(), ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        log.warn("Validation failed at {}: {}", request.getRequestURI(), message);
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(), "You do not have permission to perform this action", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        // ✅ Log the FULL stack trace so we can see the actual error
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "An unexpected error occurred", request.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }
}