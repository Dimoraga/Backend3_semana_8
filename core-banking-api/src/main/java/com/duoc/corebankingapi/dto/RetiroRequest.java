package com.duoc.corebankingapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RetiroRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0") BigDecimal monto,
        @NotBlank String canal,
        @NotBlank String usuario,
        String idempotencyKey) {
}
