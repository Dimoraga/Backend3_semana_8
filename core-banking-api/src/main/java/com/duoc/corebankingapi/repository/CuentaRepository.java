package com.duoc.corebankingapi.repository;

import com.duoc.corebankingapi.model.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    /**
     * SELECT ... FOR UPDATE: bloquea la fila para el resto de la transaccion
     * actual. Usar solo dentro de un metodo @Transactional (CuentaService)
     * que la libere al hacer commit/rollback.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :id")
    Optional<Cuenta> findByIdForUpdate(@Param("id") Long id);
}
