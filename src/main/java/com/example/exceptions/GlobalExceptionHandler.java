package com.example.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFound(ClientNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidArgumentsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArguments(InvalidArgumentsException ex) {
        // Return 401 for authentication failures, 400 for validation errors
        HttpStatus status = ex.getMessage().contains("Credentials") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return buildProblem(status, ex.getMessage());
    }

    
    @ExceptionHandler(UpdateFailedException.class)
    public ResponseEntity<ProblemDetail> handleUpdateFailed(UpdateFailedException ex) {
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ProblemDetail> buildProblem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(problem);
    }
}