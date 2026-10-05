package com.duoc.corebankingapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rastro de auditoria de cada operacion sobre una {@link Cuenta}: a que
 * canal (WEB/MOBILE/CAJERO) pertenece, tipo de movimiento y el saldo
 * resultante inmediatamente despues de aplicarlo.
 */
@Entity
@Table(name = "movimientos_cuenta")
@Data
public class MovimientoCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(nullable = false, length = 20)
    private String tipoMovimiento;

    @Column(nullable = false, length = 10)
    private String canal;

    @Column(precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(precision = 19, scale = 2)
    private BigDecimal saldoResultante;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(length = 100)
    private String usuario;

    @Column(length = 80)
    private String idempotencyKey;
}
