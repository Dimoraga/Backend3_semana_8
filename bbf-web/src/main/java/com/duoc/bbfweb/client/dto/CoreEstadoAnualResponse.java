package com.duoc.bbfweb.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espejo de com.duoc.corebankingapi.dto.EstadoCuentaAnualResponse. */
public record CoreEstadoAnualResponse(Long cuentaId, BigDecimal totalDepositos, BigDecimal totalRetiros,
        BigDecimal totalCompras, BigDecimal totalPagos, BigDecimal saldoNeto, long cantidadMovimientos,
        long cantidadAnomalias, LocalDateTime fechaGeneracion) {
}
