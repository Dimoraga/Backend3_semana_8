package com.duoc.bbfcajeros.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RetiroCajeroRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0") BigDecimal monto) {
}
