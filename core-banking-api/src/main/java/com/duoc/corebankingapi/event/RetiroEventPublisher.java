package com.duoc.corebankingapi.event;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Publica RetiroRealizadoEvent en Kafka con key = cuentaId (mismo orden por
 * cuenta). Es best-effort: cualquier falla (Kafka caido, timeout,
 * serializacion, cola llena) solo se loguea como WARN y nunca se propaga al
 * retiro.
 *
 * El envio corre en un hilo propio: con Kafka caido, send() bloquea hasta
 * max.block.ms esperando metadata, y eso no puede ocurrir en el hilo HTTP
 * (el BFF corta a los 3 s). Un solo hilo mantiene el orden de envio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroEventPublisher {

    private static final int CAPACIDAD_COLA = 1000;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(CAPACIDAD_COLA),
            runnable -> {
                Thread hilo = new Thread(runnable, "retiro-event-publisher");
                hilo.setDaemon(true);
                return hilo;
            });

    @Value("${retiros.topic}")
    private String topic;

    public void publicar(RetiroRealizadoEvent evento) {
        try {
            executor.execute(() -> enviar(evento));
        } catch (Exception e) {
            log.warn("No se pudo publicar evento de retiro: cuenta={} causa={}", evento.cuentaId(), e.getMessage());
        }
    }

    private void enviar(RetiroRealizadoEvent evento) {
        try {
            String json = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, String.valueOf(evento.cuentaId()), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("No se pudo publicar evento de retiro: cuenta={} causa={}",
                                    evento.cuentaId(), ex.getMessage());
                            return;
                        }
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.info("Evento de retiro publicado: topic={} particion={} offset={} cuenta={}",
                                metadata.topic(), metadata.partition(), metadata.offset(), evento.cuentaId());
                    });
        } catch (Exception e) {
            log.warn("No se pudo publicar evento de retiro: cuenta={} causa={}", evento.cuentaId(), e.getMessage());
        }
    }

    @PreDestroy
    void cerrar() {
        executor.shutdown();
    }
}
