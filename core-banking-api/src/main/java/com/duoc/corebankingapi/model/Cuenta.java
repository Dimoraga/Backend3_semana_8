package com.duoc.corebankingapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cuenta viva: fuente de verdad del saldo actual. Reutiliza como {@code id}
 * los mismos cuentaId ya presentes en las tablas de reporte de
 * bankbatchprocessor (cuentas_intereses / estados_cuenta_anual), pero esas
 * tablas son salida de un batch y no son seguras para mutar en vivo; esta
 * entidad es la única que se actualiza en operaciones como un retiro.
 */
@Entity
@Table(name = "cuentas")
@Data
public class Cuenta {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(nullable = false, length = 150)
    private String titular;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false, length = 5)
    private String moneda = "CLP";

    @Version
    private Long version;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
