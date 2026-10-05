package com.duoc.bbfweb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDetalleResponse(Long id, String tipoMovimiento, String canal, BigDecimal monto,
        BigDecimal saldoResultante, LocalDateTime fecha, String usuario) {
}
