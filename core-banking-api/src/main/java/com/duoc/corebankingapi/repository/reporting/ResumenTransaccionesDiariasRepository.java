package com.duoc.corebankingapi.repository.reporting;

import com.duoc.corebankingapi.model.reporting.ResumenTransaccionesDiarias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Solo lectura: ver nota en {@link com.duoc.corebankingapi.model.reporting.Transaccion}. */
public interface ResumenTransaccionesDiariasRepository extends JpaRepository<ResumenTransaccionesDiarias, Long> {

    Optional<ResumenTransaccionesDiarias> findTopByOrderByFechaGeneracionDesc();
}
