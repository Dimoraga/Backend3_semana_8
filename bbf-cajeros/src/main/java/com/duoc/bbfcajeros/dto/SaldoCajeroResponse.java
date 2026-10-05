package com.duoc.bbfcajeros.dto;

import java.math.BigDecimal;

/** El shape mas minimo de los 3 BFF: una pantalla de cajero solo muestra el numero. */
public record SaldoCajeroResponse(BigDecimal saldo) {
}
