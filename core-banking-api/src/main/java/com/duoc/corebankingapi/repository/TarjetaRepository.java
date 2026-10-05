package com.duoc.corebankingapi.repository;

import com.duoc.corebankingapi.model.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {

    Optional<Tarjeta> findByNumeroTarjetaAndActivaTrue(String numeroTarjeta);
}
