package com.duoc.corebankingapi.repository;

import com.duoc.corebankingapi.model.MovimientoCuenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    List<MovimientoCuenta> findByCuentaIdOrderByFechaDesc(Long cuentaId, Pageable pageable);
}
