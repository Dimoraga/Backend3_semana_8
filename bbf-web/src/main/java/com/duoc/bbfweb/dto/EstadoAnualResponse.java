package com.duoc.bbfweb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstadoAnualResponse(BigDecimal totalDepositos, BigDecimal totalRetiros, BigDecimal totalCompras,
        BigDecimal totalPagos, BigDecimal saldoNeto, long cantidadMovimientos, long cantidadAnomalias,
        LocalDateTime fechaGeneracion) {
}
