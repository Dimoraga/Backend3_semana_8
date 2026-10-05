package com.duoc.corebankingapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ubicacion del keystore RS256 con el que core-banking-api firma los JWT
 * (par de llaves separado del keystore TLS) y duracion de los tokens por
 * canal. Valores reales (contraseñas) viven en application-local.properties
 * (gitignored), igual que las credenciales de MySQL en bankbatchprocessor.
 */
@ConfigurationProperties(prefix = "corebanking.jwt")
@Data
public class JwtProperties {

    private String keystorePath;
    private String keystorePassword;
    private String keyAlias;
    private String keyPassword;

    private long ttlWebSeconds = 1800;
    private long ttlMobileSeconds = 900;
    private long ttlMobileRefreshSeconds = 604800;
    private long ttlCajeroSeconds = 300;
}
