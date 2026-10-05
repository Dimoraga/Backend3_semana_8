package com.duoc.corebankingapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenTarjetaRequest(
        @NotBlank String numeroTarjeta,
        @NotBlank String pin) {
}
