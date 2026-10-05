package com.duoc.corebankingapi.repository;

import com.duoc.corebankingapi.model.RetiroIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetiroIdempotenciaRepository extends JpaRepository<RetiroIdempotencia, String> {
}
