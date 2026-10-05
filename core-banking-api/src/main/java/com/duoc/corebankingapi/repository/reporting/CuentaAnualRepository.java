package com.duoc.corebankingapi.repository.reporting;

import com.duoc.corebankingapi.model.reporting.CuentaAnual;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lectura: ver nota en {@link com.duoc.corebankingapi.model.reporting.Transaccion}. */
public interface CuentaAnualRepository extends JpaRepository<CuentaAnual, Long> {
}
