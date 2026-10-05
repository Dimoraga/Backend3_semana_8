package com.duoc.bbfweb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoreBankingApiException.class)
    public ResponseEntity<Map<String, Object>> handleCoreBankingApi(CoreBankingApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Solicitud invalida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    private Map<String, Object> body(int status, String message) {
        return Map.of("timestamp", LocalDateTime.now(), "status", status, "message", message);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleNoDisponible(ServicioNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "15")
                .body(body(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "El servicio bancario no esta disponible temporalmente. Intente nuevamente en unos segundos."));
    }
}
