package com.duoc.bbfweb.dto;

import java.math.BigDecimal;

public record CuentaInfo(Long cuentaId, String numeroCuenta, String titular, String tipo, BigDecimal saldo,
        String moneda) {
}
