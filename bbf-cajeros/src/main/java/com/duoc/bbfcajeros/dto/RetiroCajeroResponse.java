package com.duoc.bbfcajeros.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Confirmacion operativa para el terminal: sin movimientoId ni metadatos de auditoria. */
public record RetiroCajeroResponse(String confirmacion, BigDecimal montoRetirado, BigDecimal saldoDisponible,
        LocalDateTime fecha) {
}
