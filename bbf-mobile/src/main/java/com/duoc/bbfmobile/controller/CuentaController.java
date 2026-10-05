package com.duoc.bbfmobile.controller;

import com.duoc.bbfmobile.client.CoreBankingApiClient;
import com.duoc.bbfmobile.client.dto.CoreMovimientoResponse;
import com.duoc.bbfmobile.client.dto.CoreSaldoResponse;
import com.duoc.bbfmobile.dto.MovimientoResumenResponse;
import com.duoc.bbfmobile.dto.SaldoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Datos esenciales y livianos: saldo trimmed y hasta 10 movimientos con campos minimos. */
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private static final int LIMITE_MOVIMIENTOS = 10;

    private final CoreBankingApiClient coreBankingApiClient;

    @GetMapping("/{cuentaId}/saldo")
    public SaldoResponse saldo(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        CoreSaldoResponse saldo = coreBankingApiClient.obtenerSaldo(cuentaId);
        return new SaldoResponse(saldo.cuentaId(), saldo.saldo(), saldo.moneda());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoResumenResponse> movimientos(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        return coreBankingApiClient.obtenerMovimientos(cuentaId, LIMITE_MOVIMIENTOS).stream()
                .map(m -> new MovimientoResumenResponse(m.tipoMovimiento(), m.monto(), m.fecha()))
                .toList();
    }

    private void verificarCuentaPropia(Long cuentaId, Jwt jwt) {
        Object claim = jwt.getClaim("cuentaId");
        Long cuentaDelToken = claim instanceof Number number ? number.longValue() : null;
        if (cuentaDelToken == null || !cuentaDelToken.equals(cuentaId)) {
            throw new AccessDeniedException("El usuario autenticado no corresponde a la cuenta solicitada");
        }
    }
}
