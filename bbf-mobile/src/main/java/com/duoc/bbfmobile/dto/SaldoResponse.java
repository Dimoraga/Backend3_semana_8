package com.duoc.bbfmobile.dto;

import java.math.BigDecimal;

/** Solo lo esencial para una pantalla movil: sin titular, sin tipo, sin timestamps. */
public record SaldoResponse(Long cuentaId, BigDecimal saldo, String moneda) {
}
