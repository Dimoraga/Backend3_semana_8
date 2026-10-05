package com.duoc.corebankingapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Deduplicacion de retiros: si un cajero reenvia la misma peticion (timeout,
 * doble click) con el mismo Idempotency-Key, se devuelve este resultado en
 * vez de aplicar el retiro dos veces.
 */
@Entity
@Table(name = "retiro_idempotencia")
@Data
public class RetiroIdempotencia {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long cuentaId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoResultante;

    @Column(nullable = false)
    private Long movimientoId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
