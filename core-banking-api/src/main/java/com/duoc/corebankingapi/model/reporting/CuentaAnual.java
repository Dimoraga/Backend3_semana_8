package com.duoc.corebankingapi.model.reporting;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espejo de solo lectura de {@code com.duoc.bankbatchprocessor.model.CuentaAnual}
 * (tabla {@code estados_cuenta_anual}). {@code cuentaId} es la propia
 * clave primaria (llave natural, no autogenerada), igual que en el
 * original. Ver nota de solo-lectura en {@link Transaccion}.
 */
@Entity
@Table(name = "estados_cuenta_anual")
@Data
public class CuentaAnual {

    @Id
    private Long cuentaId;

    private BigDecimal totalDepositos;

    private BigDecimal totalRetiros;

    private BigDecimal totalCompras;

    private BigDecimal totalPagos;

    private BigDecimal saldoNeto;

    private long cantidadMovimientos;

    private long cantidadAnomalias;

    private LocalDateTime fechaGeneracion;
}
