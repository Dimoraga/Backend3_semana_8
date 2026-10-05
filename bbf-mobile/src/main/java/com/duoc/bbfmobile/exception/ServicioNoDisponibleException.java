package com.duoc.bbfmobile.exception;

/** core-banking-api no esta disponible (caido, lento, circuito abierto o bulkhead lleno). */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }

    public ServicioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}