package com.duoc.bbfcajeros.client.dto;

/** Espejo de com.duoc.corebankingapi.dto.ErrorResponse (solo los campos que nos interesa reenviar). */
public record CoreErrorResponse(int status, String error, String message) {
}
