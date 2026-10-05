package com.duoc.corebankingapi.controller;

import com.duoc.corebankingapi.dto.MovimientoResponse;
import com.duoc.corebankingapi.dto.RetiroRequest;
import com.duoc.corebankingapi.dto.RetiroResultado;
import com.duoc.corebankingapi.dto.SaldoResponse;
import com.duoc.corebankingapi.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/{cuentaId}/saldo")
    public SaldoResponse saldo(@PathVariable Long cuentaId) {
        return cuentaService.consultarSaldo(cuentaId);
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoResponse> movimientos(@PathVariable Long cuentaId,
            @RequestParam(defaultValue = "20") int limit) {
        return cuentaService.movimientosRecientes(cuentaId, limit);
    }

    @PostMapping("/{cuentaId}/retiros")
    public RetiroResultado retirar(@PathVariable Long cuentaId, @Valid @RequestBody RetiroRequest request) {
        return cuentaService.retirar(cuentaId, request);
    }
}
