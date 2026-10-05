package com.duoc.bbfweb.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espejo de com.duoc.corebankingapi.dto.ResumenDiarioResponse. */
public record CoreResumenDiarioResponse(LocalDateTime fechaGeneracion, long totalProcesadas, long totalAnomalias,
        long totalCreditos, long totalDebitos, BigDecimal montoTotalCreditos, BigDecimal montoTotalDebitos) {
}
