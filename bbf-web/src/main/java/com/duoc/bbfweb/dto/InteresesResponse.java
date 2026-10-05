package com.duoc.bbfweb.dto;

import java.math.BigDecimal;

public record InteresesResponse(String tipo, BigDecimal saldo, BigDecimal tasaAplicada, BigDecimal saldoFinal,
        boolean anomalia) {
}
