package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;

/** Respuesta completa de saldo/detalle de cuenta; cada BFF recorta lo que necesita. */
public record SaldoResponse(
        Long cuentaId,
        String numeroCuenta,
        String titular,
        String tipo,
        BigDecimal saldo,
        String moneda) {
}
