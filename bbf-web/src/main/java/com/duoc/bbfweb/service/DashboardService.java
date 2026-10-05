package com.duoc.bbfweb.service;

import com.duoc.bbfweb.client.CoreBankingApiClient;
import com.duoc.bbfweb.client.dto.CoreEstadoAnualResponse;
import com.duoc.bbfweb.client.dto.CoreInteresResponse;
import com.duoc.bbfweb.client.dto.CoreMovimientoResponse;
import com.duoc.bbfweb.client.dto.CoreResumenDiarioResponse;
import com.duoc.bbfweb.client.dto.CoreSaldoResponse;
import com.duoc.bbfweb.dto.CuentaInfo;
import com.duoc.bbfweb.dto.DashboardResponse;
import com.duoc.bbfweb.dto.EstadoAnualResponse;
import com.duoc.bbfweb.dto.InteresesResponse;
import com.duoc.bbfweb.dto.MovimientoDetalleResponse;
import com.duoc.bbfweb.dto.ResumenSistemaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Compone en un solo response las 5 llamadas a core-banking-api que
 * necesita una vista de escritorio con UI compleja: saldo, movimientos
 * recientes, estado de cuenta anual, intereses aplicados y el resumen
 * diario del sistema. Los reportes historicos son opcionales (una cuenta
 * viva puede no tener historial en bankbatchprocessor todavia).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MOVIMIENTOS_DASHBOARD = 50;

    private final CoreBankingApiClient coreBankingApiClient;

    public DashboardResponse construir(Long cuentaId) {
        CoreSaldoResponse saldo = coreBankingApiClient.obtenerSaldo(cuentaId);
        var movimientos = coreBankingApiClient.obtenerMovimientos(cuentaId, MOVIMIENTOS_DASHBOARD).stream()
                .map(this::toMovimientoDetalle)
                .toList();
        EstadoAnualResponse estadoAnual = coreBankingApiClient.obtenerEstadoAnual(cuentaId)
                .map(this::toEstadoAnual)
                .orElse(null);
        InteresesResponse intereses = coreBankingApiClient.obtenerIntereses(cuentaId)
                .map(this::toIntereses)
                .orElse(null);
        ResumenSistemaResponse resumenSistema = coreBankingApiClient.obtenerResumenDiario()
                .map(this::toResumenSistema)
                .orElse(null);

        return new DashboardResponse(toCuentaInfo(saldo), movimientos, estadoAnual, intereses, resumenSistema);
    }

    private CuentaInfo toCuentaInfo(CoreSaldoResponse s) {
        return new CuentaInfo(s.cuentaId(), s.numeroCuenta(), s.titular(), s.tipo(), s.saldo(), s.moneda());
    }

    private MovimientoDetalleResponse toMovimientoDetalle(CoreMovimientoResponse m) {
        return new MovimientoDetalleResponse(m.id(), m.tipoMovimiento(), m.canal(), m.monto(),
                m.saldoResultante(), m.fecha(), m.usuario());
    }

    private EstadoAnualResponse toEstadoAnual(CoreEstadoAnualResponse e) {
        return new EstadoAnualResponse(e.totalDepositos(), e.totalRetiros(), e.totalCompras(), e.totalPagos(),
                e.saldoNeto(), e.cantidadMovimientos(), e.cantidadAnomalias(), e.fechaGeneracion());
    }

    private InteresesResponse toIntereses(CoreInteresResponse i) {
        return new InteresesResponse(i.tipo(), i.saldo(), i.tasaAplicada(), i.saldoFinal(), i.anomalia());
    }

    private ResumenSistemaResponse toResumenSistema(CoreResumenDiarioResponse r) {
        return new ResumenSistemaResponse(r.fechaGeneracion(), r.totalProcesadas(), r.totalAnomalias(),
                r.totalCreditos(), r.totalDebitos(), r.montoTotalCreditos(), r.montoTotalDebitos());
    }
}
