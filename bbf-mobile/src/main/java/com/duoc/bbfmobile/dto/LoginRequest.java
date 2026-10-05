package com.duoc.bbfmobile.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usuario, @NotBlank String password, @NotBlank String deviceId) {
}
