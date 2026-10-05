package com.duoc.corebankingapi.service;

import com.duoc.corebankingapi.dto.EstadoCuentaAnualResponse;
import com.duoc.corebankingapi.dto.InteresResponse;
import com.duoc.corebankingapi.dto.MovimientoAnualResponse;
import com.duoc.corebankingapi.dto.ResumenDiarioResponse;
import com.duoc.corebankingapi.model.reporting.CuentaAnual;
import com.duoc.corebankingapi.model.reporting.CuentaInteres;
import com.duoc.corebankingapi.model.reporting.MovimientoCuentaAnual;
import com.duoc.corebankingapi.model.reporting.ResumenTransaccionesDiarias;
import com.duoc.corebankingapi.repository.reporting.CuentaAnualRepository;
import com.duoc.corebankingapi.repository.reporting.CuentaInteresRepository;
import com.duoc.corebankingapi.repository.reporting.MovimientoCuentaAnualRepository;
import com.duoc.corebankingapi.repository.reporting.ResumenTransaccionesDiariasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lecturas sobre las tablas de reporte generadas por bankbatchprocessor.
 * Solo consultas: este servicio nunca escribe en esas tablas (ver nota en
 * com.duoc.corebankingapi.model.reporting.Transaccion).
 */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final CuentaAnualRepository cuentaAnualRepository;
    private final CuentaInteresRepository cuentaInteresRepository;
    private final MovimientoCuentaAnualRepository movimientoCuentaAnualRepository;
    private final ResumenTransaccionesDiariasRepository resumenTransaccionesDiariasRepository;

    @Transactional(readOnly = true)
    public Optional<EstadoCuentaAnualResponse> estadoCuentaAnual(Long cuentaId) {
        return cuentaAnualRepository.findById(cuentaId).map(this::toEstadoResponse);
    }

    @Transactional(readOnly = true)
    public Optional<InteresResponse> intereses(Long cuentaId) {
        return cuentaInteresRepository.findTopByCuentaIdOrderByIdDesc(cuentaId).map(this::toInteresResponse);
    }

    @Transactional(readOnly = true)
    public List<MovimientoAnualResponse> movimientosAnuales(Long cuentaId) {
        return movimientoCuentaAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .map(this::toMovimientoAnualResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ResumenDiarioResponse> resumenDiario() {
        return resumenTransaccionesDiariasRepository.findTopByOrderByFechaGeneracionDesc()
                .map(this::toResumenResponse);
    }

    private EstadoCuentaAnualResponse toEstadoResponse(CuentaAnual c) {
        return new EstadoCuentaAnualResponse(c.getCuentaId(), c.getTotalDepositos(), c.getTotalRetiros(),
                c.getTotalCompras(), c.getTotalPagos(), c.getSaldoNeto(), c.getCantidadMovimientos(),
                c.getCantidadAnomalias(), c.getFechaGeneracion());
    }

    private InteresResponse toInteresResponse(CuentaInteres c) {
        return new InteresResponse(c.getCuentaId(), c.getTipo(), c.getSaldo(), c.getTasaAplicada(),
                c.getSaldoFinal(), c.isAnomalia());
    }

    private MovimientoAnualResponse toMovimientoAnualResponse(MovimientoCuentaAnual m) {
        return new MovimientoAnualResponse(m.getId(), m.getFecha(), m.getTipoMovimiento(), m.getMonto(),
                m.getDescripcion(), m.isAnomalia());
    }

    private ResumenDiarioResponse toResumenResponse(ResumenTransaccionesDiarias r) {
        return new ResumenDiarioResponse(r.getFechaGeneracion(), r.getTotalProcesadas(), r.getTotalAnomalias(),
                r.getTotalCreditos(), r.getTotalDebitos(), r.getMontoTotalCreditos(), r.getMontoTotalDebitos());
    }
}
