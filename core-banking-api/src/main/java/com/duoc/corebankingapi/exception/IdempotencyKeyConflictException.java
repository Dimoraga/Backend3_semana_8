package com.duoc.corebankingapi.exception;

/**
 * Se reutilizo un Idempotency-Key con un monto distinto al de la solicitud
 * original: no se puede asumir que es un reintento legitimo del mismo retiro.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("El Idempotency-Key '" + idempotencyKey
                + "' ya fue usado con un monto distinto");
    }
}
