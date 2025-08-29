package com.luismunozse.reservalago.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,String> handleConflict(IllegalStateException ex){ return Map.of("error", ex.getMessage()); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> handleBadRequest(IllegalArgumentException ex){ return Map.of("error", ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> handleValidation(MethodArgumentNotValidException ex){
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField()+": "+e.getDefaultMessage()).findFirst().orElse("Datos inválidos");
        return Map.of("error", msg);
    }
}
