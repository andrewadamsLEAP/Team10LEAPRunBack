package com.example.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex) {
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database operation failed. The transactions table schema may be out of sync with the application mapper."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, ServletWebRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null ? "anonymous" : authentication.getName();
        String authorities = authentication == null ? "none" : authentication.getAuthorities().toString();
        String path = request.getRequest().getRequestURI();

        logger.warn("Access denied for principal '{}' on {}. Granted authorities: {}. Reason: {}",
                principal, path, authorities, ex.getMessage());

        return buildProblem(HttpStatus.FORBIDDEN,
                "Access denied. The authenticated user does not have the required role for this endpoint.");
    }

    private ResponseEntity<ProblemDetail> buildProblem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(problem);
    }
}