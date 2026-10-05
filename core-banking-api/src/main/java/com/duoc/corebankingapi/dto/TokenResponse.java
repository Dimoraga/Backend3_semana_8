package com.duoc.corebankingapi.dto;

public record TokenResponse(
        String token,
        String refreshToken,
        long expiresInSeconds,
        String canal,
        String roles,
        Long cuentaId) {
}
