package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;

public record InteresResponse(
        Long cuentaId,
        String tipo,
        BigDecimal saldo,
        BigDecimal tasaAplicada,
        BigDecimal saldoFinal,
        boolean anomalia) {
}
