package com.duoc.bbfcajeros.controller;

import com.duoc.bbfcajeros.client.CoreBankingApiClient;
import com.duoc.bbfcajeros.client.dto.CoreRetiroRequest;
import com.duoc.bbfcajeros.client.dto.CoreRetiroResultado;
import com.duoc.bbfcajeros.client.dto.CoreSaldoResponse;
import com.duoc.bbfcajeros.dto.RetiroCajeroRequest;
import com.duoc.bbfcajeros.dto.RetiroCajeroResponse;
import com.duoc.bbfcajeros.dto.SaldoCajeroResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones criticas del canal cajero: consulta de saldo y retiro, ambas minimas en payload. */
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private static final String CANAL = "CAJERO";

    private final CoreBankingApiClient coreBankingApiClient;

    @GetMapping("/{cuentaId}/saldo")
    public SaldoCajeroResponse saldo(@PathVariable Long cuentaId, @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        CoreSaldoResponse saldo = coreBankingApiClient.obtenerSaldo(cuentaId);
        return new SaldoCajeroResponse(saldo.saldo());
    }

    @PostMapping("/{cuentaId}/retiros")
    public RetiroCajeroResponse retirar(@PathVariable Long cuentaId,
            @Valid @RequestBody RetiroCajeroRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        verificarCuentaPropia(cuentaId, jwt);
        CoreRetiroRequest coreRequest = new CoreRetiroRequest(request.monto(), CANAL, jwt.getSubject(), idempotencyKey);
        CoreRetiroResultado resultado = coreBankingApiClient.retirar(cuentaId, coreRequest);
        return new RetiroCajeroResponse("OK", request.monto(), resultado.saldoNuevo(), resultado.fecha());
    }

    /** El token queda ligado a una cuenta (claim cuentaId) desde el login con tarjeta; nunca se confia en el path. */
    private void verificarCuentaPropia(Long cuentaId, Jwt jwt) {
        Object claim = jwt.getClaim("cuentaId");
        Long cuentaDelToken = claim instanceof Number number ? number.longValue() : null;
        if (cuentaDelToken == null || !cuentaDelToken.equals(cuentaId)) {
            throw new AccessDeniedException("La tarjeta autenticada no corresponde a la cuenta solicitada");
        }
    }
}
