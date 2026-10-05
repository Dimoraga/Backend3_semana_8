package com.duoc.bbfweb.dto;

import java.util.List;

/**
 * Composicion completa de 5 llamadas a core-banking-api en un solo
 * response: cuenta + movimientos + estado anual + intereses + resumen del
 * sistema, sin recortar ni paginar. Es el punto de contraste directo con
 * los shapes minimos de bbf-mobile/bbf-cajeros para el mismo dominio.
 */
public record DashboardResponse(CuentaInfo cuenta, List<MovimientoDetalleResponse> movimientosRecientes,
        EstadoAnualResponse estadoAnual, InteresesResponse interesesAplicados,
        ResumenSistemaResponse resumenDiarioSistema) {
}
