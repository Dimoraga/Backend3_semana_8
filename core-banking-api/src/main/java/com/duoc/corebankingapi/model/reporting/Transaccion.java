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
 * Espejo de solo lectura de {@code com.duoc.bankbatchprocessor.model.Transaccion}
 * (tabla {@code transacciones}, poblada por bankBatchProcessor). NUNCA usar
 * save()/delete() contra el repositorio de esta entidad desde este servicio:
 * es salida de un batch ajeno, no un dato que core-banking-api sea dueno.
 */
@Entity
@Table(name = "transacciones")
@Data
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transaccionId;

    private LocalDate fecha;

    private String fechaOriginal;

    private BigDecimal monto;

    private String tipo;

    private boolean anomalia;

    @Column(length = 500)
    private String observacion;
}
