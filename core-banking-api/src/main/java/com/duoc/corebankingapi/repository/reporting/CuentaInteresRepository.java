package com.duoc.corebankingapi.repository.reporting;

import com.duoc.corebankingapi.model.reporting.CuentaInteres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Solo lectura: ver nota en {@link com.duoc.corebankingapi.model.reporting.Transaccion}. */
public interface CuentaInteresRepository extends JpaRepository<CuentaInteres, Long> {

    Optional<CuentaInteres> findTopByCuentaIdOrderByIdDesc(Long cuentaId);
}
