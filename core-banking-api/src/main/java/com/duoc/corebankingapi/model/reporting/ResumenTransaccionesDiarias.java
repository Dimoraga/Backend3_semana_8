package com.duoc.corebankingapi.model.reporting;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espejo de solo lectura de
 * {@code com.duoc.bankbatchprocessor.model.ResumenTransaccionesDiarias}
 * (tabla {@code resumen_transacciones_diarias}). Ver nota de solo-lectura
 * en {@link Transaccion}.
 */
@Entity
@Table(name = "resumen_transacciones_diarias")
@Data
public class ResumenTransaccionesDiarias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaGeneracion;

    private long totalProcesadas;

    private long totalAnomalias;

    private long totalCreditos;

    private long totalDebitos;

    private BigDecimal montoTotalCreditos;

    private BigDecimal montoTotalDebitos;
}
