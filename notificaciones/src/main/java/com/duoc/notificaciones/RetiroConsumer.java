package com.duoc.notificaciones;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notificaciones.topic}")
    public void onRetiro(String mensaje) {
        RetiroRealizadoEvent evento = objectMapper.readValue(mensaje, RetiroRealizadoEvent.class);
        log.info("NOTIFICACION -> Retiro de ${} en la cuenta {}. Saldo disponible: ${} ({})",
                evento.monto(), evento.cuentaId(), evento.saldoDisponible(), evento.fecha());
    }
}
