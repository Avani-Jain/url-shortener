package com.project.url_shortener.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFoundException(UrlNotFoundException ex, HttpServletRequest request) {
        //HttpStatus.NOT_FOUND is an Object type of enum having status code
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        //BindingResult is an interface that acts as a container for data-binding and validation errors. When it is tied to an exception, it usually means a validation failed on an incoming request object (like a form submission or a JSON body), and Spring captured those errors into a BindingResult inside a specific validation exception.
        //When you do not manually inspect errors in your controller, Spring stops the request execution and automatically throws a validation exception. The BindingResult instance is embedded inside that thrown exception.The type of exception depends on how the request data was received:  MethodArgumentNotValidException: Thrown when validation fails on a payload marked with @RequestBody (typically JSON in REST APIs). BindException: Thrown when validation fails on query parameters or form data marked with @ModelAttribute. Both exceptions expose a .getBindingResult() method so you can extract and inspect the exact validation failures programmatically.
        //.getFieldErrors() --> looping through errors to get field names and messages
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", message,  request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),  "Internal Server Error", "Something went wrong. Please try again later.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
