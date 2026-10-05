package com.duoc.bbfcajeros.dto;

/** El cajero solo necesita el token para operar y la cuenta a la que quedo ligada la tarjeta. */
public record LoginResponse(String token, long expiresInSeconds, Long cuentaId) {
}
