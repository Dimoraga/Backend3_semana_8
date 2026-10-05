package com.duoc.corebankingapi.model.reporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Espejo de solo lectura de {@code com.duoc.bankbatchprocessor.model.CuentaInteres}
 * (tabla {@code cuentas_intereses}). El batch inserta una fila nueva cada
 * vez que corre (no hay unicidad por cuentaId), por eso el repositorio
 * expone consultas ordenadas por id descendente para tomar la mas reciente.
 * Ver nota de solo-lectura en {@link Transaccion}.
 */
@Entity
@Table(name = "cuentas_intereses")
@Data
public class CuentaInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cuentaId;

    private String nombre;

    private BigDecimal saldo;

    private Integer edad;

    private String tipo;

    private BigDecimal tasaAplicada;

    private BigDecimal saldoFinal;

    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
