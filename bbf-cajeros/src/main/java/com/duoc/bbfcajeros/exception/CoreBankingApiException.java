package com.duoc.bbfcajeros.exception;

import org.springframework.http.HttpStatusCode;

/** Envuelve un error devuelto por core-banking-api para reenviarlo tal cual al cliente del BFF. */
public class CoreBankingApiException extends RuntimeException {

    private final HttpStatusCode status;

    public CoreBankingApiException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
