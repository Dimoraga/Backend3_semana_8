package com.duoc.bbfmobile.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Campos minimos: sin canal, sin saldoResultante, sin usuario -- solo lo que una lista movil necesita mostrar. */
public record MovimientoResumenResponse(String tipoMovimiento, BigDecimal monto, LocalDateTime fecha) {
}
