package com.duoc.bbfcajeros.client.dto;

import java.math.BigDecimal;

/** Espejo de com.duoc.corebankingapi.dto.RetiroRequest. */
public record CoreRetiroRequest(BigDecimal monto, String canal, String usuario, String idempotencyKey) {
}
