package com.duoc.bbfweb.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usuario, @NotBlank String password) {
}
