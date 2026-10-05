package com.duoc.corebankingapi.event;

import java.math.BigDecimal;

/** Evento publicado en Kafka cuando un retiro queda confirmado en BD (lo consume notificaciones). */
public record RetiroRealizadoEvent(Long cuentaId, BigDecimal monto, BigDecimal saldoDisponible, String fecha) {
}
