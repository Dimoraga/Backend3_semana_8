package com.duoc.bbfcajeros.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String numeroTarjeta, @NotBlank String pin) {
}
