package com.duoc.corebankingapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Llave compartida para el header X-Internal-Api-Key: confianza
 * servicio-a-servicio entre core-banking-api y los 3 BFF, distinta de los
 * JWT de usuario final. El valor real vive en application-local.properties
 * (gitignored) y debe coincidir con el configurado en cada BFF.
 */
@ConfigurationProperties(prefix = "corebanking.security")
@Data
public class InternalApiKeyProperties {

    private String internalApiKey;
}
