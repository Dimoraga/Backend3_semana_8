package com.duoc.bbfweb.client.dto;

import java.math.BigDecimal;

/** Espejo de com.duoc.corebankingapi.dto.InteresResponse. */
public record CoreInteresResponse(Long cuentaId, String tipo, BigDecimal saldo, BigDecimal tasaAplicada,
        BigDecimal saldoFinal, boolean anomalia) {
}
