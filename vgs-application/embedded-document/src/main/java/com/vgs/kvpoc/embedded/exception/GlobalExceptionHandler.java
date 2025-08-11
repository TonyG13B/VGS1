
package com.vgs.kvpoc.embedded.exception;

import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentNotFound(
            DocumentNotFoundException ex, WebRequest request) {
        
        Map<String, Object> response = createErrorResponse(
            "DOCUMENT_NOT_FOUND", 
            "The requested document was not found", 
            HttpStatus.NOT_FOUND
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(
            TimeoutException ex, WebRequest request) {
        
        Map<String, Object> response = createErrorResponse(
            "REQUEST_TIMEOUT", 
            "The request timed out. Please try again.", 
            HttpStatus.REQUEST_TIMEOUT
        );
        
        return new ResponseEntity<>(response, HttpStatus.REQUEST_TIMEOUT);
    }
    
    @ExceptionHandler(CouchbaseException.class)
    public ResponseEntity<Map<String, Object>> handleCouchbaseException(
            CouchbaseException ex, WebRequest request) {
        
        Map<String, Object> response = createErrorResponse(
            "DATABASE_ERROR", 
            "A database error occurred: " + ex.getMessage(), 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        Map<String, Object> response = createErrorResponse(
            "INTERNAL_ERROR", 
            "An unexpected error occurred", 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private Map<String, Object> createErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "code", code,
            "message", message,
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value()
        ));
        return response;
    }
}
