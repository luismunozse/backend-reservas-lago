package com.luismunozse.reservalago.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,String> handleConflict(IllegalStateException ex){
        log.warn("Conflicto: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> handleBadRequest(IllegalArgumentException ex){
        log.warn("Solicitud inválida: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> handleValidation(MethodArgumentNotValidException ex){
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField()+": "+e.getDefaultMessage()).findFirst().orElse("Datos inválidos");
        log.warn("Validación fallida: {}", msg);
        return Map.of("error", msg);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : "Error en la solicitud";
        log.warn("ResponseStatusException: status={}, message={}", status, message);
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        log.warn("DataIntegrityViolation: {}", message);

        // Detectar constraint de DNI+fecha duplicado
        if (message.contains("ux_reservations_date_dni")) {
            return Map.of("error", "Ya existe una reserva con ese DNI para esa fecha.");
        }
        // Detectar email duplicado
        if (message.contains("users") && message.contains("email")) {
            return Map.of("error", "El email ya está registrado.");
        }
        // Mensaje genérico para otras violaciones de integridad
        return Map.of("error", "Los datos ingresados ya existen o violan una restricción.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Map<String,String> handleGeneric(Exception ex){
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return Map.of("error", "Error interno del servidor");
    }
}
