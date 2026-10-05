package com.duoc.bbfcajeros.client.dto;

import java.math.BigDecimal;

/** Espejo de com.duoc.corebankingapi.dto.SaldoResponse. */
public record CoreSaldoResponse(Long cuentaId, String numeroCuenta, String titular, String tipo,
        BigDecimal saldo, String moneda) {
}
