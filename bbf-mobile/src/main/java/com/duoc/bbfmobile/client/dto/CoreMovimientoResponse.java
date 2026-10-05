package com.duoc.bbfmobile.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espejo de com.duoc.corebankingapi.dto.MovimientoResponse. */
public record CoreMovimientoResponse(Long id, String tipoMovimiento, String canal, BigDecimal monto,
        BigDecimal saldoResultante, LocalDateTime fecha, String usuario) {
}
