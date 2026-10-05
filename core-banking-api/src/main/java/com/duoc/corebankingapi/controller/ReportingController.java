package com.duoc.corebankingapi.controller;

import com.duoc.corebankingapi.dto.EstadoCuentaAnualResponse;
import com.duoc.corebankingapi.dto.InteresResponse;
import com.duoc.corebankingapi.dto.MovimientoAnualResponse;
import com.duoc.corebankingapi.dto.ResumenDiarioResponse;
import com.duoc.corebankingapi.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lecturas sobre los datos generados por bankbatchprocessor, usadas solo por el dashboard de bbf-web. */
@RestController
@RequestMapping("/internal/reportes")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/cuentas/{cuentaId}/estado-anual")
    public ResponseEntity<EstadoCuentaAnualResponse> estadoAnual(@PathVariable Long cuentaId) {
        return reportingService.estadoCuentaAnual(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cuentas/{cuentaId}/intereses")
    public ResponseEntity<InteresResponse> intereses(@PathVariable Long cuentaId) {
        return reportingService.intereses(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos-anuales")
    public List<MovimientoAnualResponse> movimientosAnuales(@PathVariable Long cuentaId) {
        return reportingService.movimientosAnuales(cuentaId);
    }

    @GetMapping("/resumen-diario")
    public ResponseEntity<ResumenDiarioResponse> resumenDiario() {
        return reportingService.resumenDiario()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
