package com.duoc.bbfcajeros.client.dto;

/** Espejo de com.duoc.corebankingapi.dto.TokenResponse. */
public record CoreTokenResponse(String token, String refreshToken, long expiresInSeconds, String canal,
        String roles, Long cuentaId) {
}
