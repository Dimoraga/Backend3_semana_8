package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        String tipoMovimiento,
        String canal,
        BigDecimal monto,
        BigDecimal saldoResultante,
        LocalDateTime fecha,
        String usuario) {
}
