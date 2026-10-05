package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResumenDiarioResponse(
        LocalDateTime fechaGeneracion,
        long totalProcesadas,
        long totalAnomalias,
        long totalCreditos,
        long totalDebitos,
        BigDecimal montoTotalCreditos,
        BigDecimal montoTotalDebitos) {
}
