package com.duoc.corebankingapi.exception;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales invalidas");
    }
}
