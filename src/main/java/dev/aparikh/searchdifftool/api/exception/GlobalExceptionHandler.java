package dev.aparikh.searchdifftool.api.exception;

import dev.aparikh.searchdifftool.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SearchDiffException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearchDiffException(SearchDiffException e) {
        log.error("Search diff error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("SEARCH_ERROR", e.getMessage()));
    }
    
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(Exception e) {
        Map<String, String> errors = new HashMap<>();
        
        // MethodArgumentNotValidException is a BindException. Only field errors carry a field name;
        // object-level errors (e.g. class-level constraints) must not be cast to FieldError.
        if (e instanceof BindException bindException) {
            for (FieldError error : bindException.getBindingResult().getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
        }
        
        ApiResponse<Map<String, String>> body = ApiResponse.error("VALIDATION_ERROR", "Invalid request parameters");
        body.setData(errors);
        return ResponseEntity.badRequest().body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}