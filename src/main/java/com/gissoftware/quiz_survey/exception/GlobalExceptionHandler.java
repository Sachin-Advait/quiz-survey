package com.gissoftware.quiz_survey.exception;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponseDTO<String> handleAccessDenied(AccessDeniedException ex) {
        return new ApiResponseDTO<>(false, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponseDTO<String> handleConflict(IllegalStateException ex) {
        return new ApiResponseDTO<>(false, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseDTO<String> handleBadRequest(IllegalArgumentException ex) {
        return new ApiResponseDTO<>(false, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseDTO<String> handleOtherExceptions(Exception ex) {
        return new ApiResponseDTO<>(false, "An unexpected error occurred", null);
    }
}

