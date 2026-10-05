package com.duoc.corebankingapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenUsuarioRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String canal) {
}
