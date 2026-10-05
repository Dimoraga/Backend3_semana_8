package com.duoc.corebankingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoAnualResponse(
        Long id,
        LocalDate fecha,
        String tipoMovimiento,
        BigDecimal monto,
        String descripcion,
        boolean anomalia) {
}
