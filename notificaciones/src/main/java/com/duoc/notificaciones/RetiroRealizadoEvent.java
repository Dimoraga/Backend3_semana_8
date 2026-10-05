package com.duoc.notificaciones;

import java.math.BigDecimal;

/** Evento que core-banking-api publica cuando se concreta un retiro. */
public record RetiroRealizadoEvent(Long cuentaId, BigDecimal monto, BigDecimal saldoDisponible, String fecha) {
}
