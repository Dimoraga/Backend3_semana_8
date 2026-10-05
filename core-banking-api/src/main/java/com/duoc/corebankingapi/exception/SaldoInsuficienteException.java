package com.duoc.corebankingapi.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(Long cuentaId, BigDecimal saldoDisponible, BigDecimal montoSolicitado) {
        super("Saldo insuficiente en cuenta " + cuentaId + ": disponible " + saldoDisponible
                + ", solicitado " + montoSolicitado);
    }
}
