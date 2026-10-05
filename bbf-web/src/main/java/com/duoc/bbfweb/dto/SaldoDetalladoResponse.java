package com.duoc.bbfweb.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** El shape mas completo de los 3 BFF: todo el detalle de la cuenta, sin recortes. */
public record SaldoDetalladoResponse(Long cuentaId, String numeroCuenta, String titular, String tipo,
        BigDecimal saldo, String moneda, LocalDateTime fechaConsulta) {
}
