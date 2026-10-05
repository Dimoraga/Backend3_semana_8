package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstadoCuentaAnualResponse(
        Long cuentaId,
        BigDecimal totalDepositos,
        BigDecimal totalRetiros,
        BigDecimal totalCompras,
        BigDecimal totalPagos,
        BigDecimal saldoNeto,
        long cantidadMovimientos,
        long cantidadAnomalias,
        LocalDateTime fechaGeneracion) {
}
