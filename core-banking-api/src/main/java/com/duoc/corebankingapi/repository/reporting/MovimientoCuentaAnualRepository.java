package com.duoc.corebankingapi.repository.reporting;

import com.duoc.corebankingapi.model.reporting.MovimientoCuentaAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Solo lectura: ver nota en {@link com.duoc.corebankingapi.model.reporting.Transaccion}. */
public interface MovimientoCuentaAnualRepository extends JpaRepository<MovimientoCuentaAnual, Long> {

    List<MovimientoCuentaAnual> findByCuentaIdOrderByFechaDesc(Long cuentaId);
}
