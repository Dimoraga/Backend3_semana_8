package com.duoc.bbfcajeros.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espejo de com.duoc.corebankingapi.dto.RetiroResultado. */
public record CoreRetiroResultado(Long movimientoId, BigDecimal saldoAnterior, BigDecimal saldoNuevo,
        LocalDateTime fecha) {
}
