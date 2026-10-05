package com.duoc.corebankingapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Credenciales de acceso para el canal CAJERO (numero de tarjeta + PIN).
 * {@code intentosFallidos} queda disponible para un futuro bloqueo tras
 * N intentos, no aplicado todavia (fuera del alcance de esta entrega).
 */
@Entity
@Table(name = "tarjetas")
@Data
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroTarjeta;

    @Column(nullable = false, length = 100)
    private String pinHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(nullable = false)
    private int intentosFallidos = 0;
}
