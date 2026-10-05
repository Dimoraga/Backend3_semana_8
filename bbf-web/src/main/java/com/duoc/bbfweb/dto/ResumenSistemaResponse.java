package com.duoc.bbfweb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResumenSistemaResponse(LocalDateTime fechaGeneracion, long totalProcesadas, long totalAnomalias,
        long totalCreditos, long totalDebitos, BigDecimal montoTotalCreditos, BigDecimal montoTotalDebitos) {
}
