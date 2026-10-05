package com.duoc.corebankingapi.exception;

public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(Long cuentaId) {
        super("No existe la cuenta " + cuentaId);
    }
}
