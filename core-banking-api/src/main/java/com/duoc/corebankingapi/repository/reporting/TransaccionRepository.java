package com.duoc.corebankingapi.repository.reporting;

import com.duoc.corebankingapi.model.reporting.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lectura: ver nota en {@link Transaccion}. No usar save()/delete() aqui. */
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
}
