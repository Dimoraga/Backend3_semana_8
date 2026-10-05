package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RetiroResultado(
        Long movimientoId,
        BigDecimal saldoAnterior,
        BigDecimal saldoNuevo,
        LocalDateTime fecha) {
}
