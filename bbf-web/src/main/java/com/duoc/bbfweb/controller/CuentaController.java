package com.duoc.bbfweb.controller;

import com.duoc.bbfweb.client.CoreBankingApiClient;
import com.duoc.bbfweb.client.dto.CoreEstadoAnualResponse;
import com.duoc.bbfweb.client.dto.CoreMovimientoResponse;
import com.duoc.bbfweb.client.dto.CoreSaldoResponse;
import com.duoc.bbfweb.dto.DashboardResponse;
import com.duoc.bbfweb.dto.EstadoAnualResponse;
import com.duoc.bbfweb.dto.MovimientoDetalleResponse;
import com.duoc.bbfweb.dto.SaldoDetalladoResponse;
import com.duoc.bbfweb.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Datos completos, sin recortar, pensados para una UI de escritorio con varias secciones a la vez. */
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private static final int MOVIMIENTOS_COMPLETOS = 50;

    private final CoreBankingApiClient coreBankingApiClient;
    private final DashboardService dashboardService;

    @GetMapping("/{cuentaId}/dashboard")
    public DashboardResponse dashboard(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        return dashboardService.construir(cuentaId);
    }

    @GetMapping("/{cuentaId}/saldo")
    public SaldoDetalladoResponse saldo(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        CoreSaldoResponse saldo = coreBankingApiClient.obtenerSaldo(cuentaId);
        return new SaldoDetalladoResponse(saldo.cuentaId(), saldo.numeroCuenta(), saldo.titular(), saldo.tipo(),
                saldo.saldo(), saldo.moneda(), LocalDateTime.now());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoDetalleResponse> movimientos(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        return coreBankingApiClient.obtenerMovimientos(cuentaId, MOVIMIENTOS_COMPLETOS).stream()
                .map(this::toMovimientoDetalle)
                .toList();
    }

    @GetMapping("/{cuentaId}/estado-anual")
    public ResponseEntity<EstadoAnualResponse> estadoAnual(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        return coreBankingApiClient.obtenerEstadoAnual(cuentaId)
                .map(this::toEstadoAnual)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private MovimientoDetalleResponse toMovimientoDetalle(CoreMovimientoResponse m) {
        return new MovimientoDetalleResponse(m.id(), m.tipoMovimiento(), m.canal(), m.monto(),
                m.saldoResultante(), m.fecha(), m.usuario());
    }

    private EstadoAnualResponse toEstadoAnual(CoreEstadoAnualResponse e) {
        return new EstadoAnualResponse(e.totalDepositos(), e.totalRetiros(), e.totalCompras(), e.totalPagos(),
                e.saldoNeto(), e.cantidadMovimientos(), e.cantidadAnomalias(), e.fechaGeneracion());
    }

    private void verificarCuentaPropia(Long cuentaId, Jwt jwt) {
        Object claim = jwt.getClaim("cuentaId");
        Long cuentaDelToken = claim instanceof Number number ? number.longValue() : null;
        if (cuentaDelToken == null || !cuentaDelToken.equals(cuentaId)) {
            throw new AccessDeniedException("El usuario autenticado no corresponde a la cuenta solicitada");
        }
    }
}
