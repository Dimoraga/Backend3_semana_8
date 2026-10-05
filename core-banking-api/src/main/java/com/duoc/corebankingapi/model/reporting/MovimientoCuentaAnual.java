package com.duoc.corebankingapi.model.reporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Espejo de solo lectura de
 * {@code com.duoc.bankbatchprocessor.model.MovimientoCuentaAnual}
 * (tabla {@code movimientos_cuenta_anual}). Ver nota de solo-lectura en
 * {@link Transaccion}.
 */
@Entity
@Table(name = "movimientos_cuenta_anual")
@Data
public class MovimientoCuentaAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cuentaId;

    private LocalDate fecha;

    private String fechaOriginal;

    private String tipoMovimiento;

    private BigDecimal monto;

    @Column(length = 300)
    private String descripcion;

    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
