package com.duoc.bbfcajeros.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/**
 * Valida JWT localmente con la llave publica RS256 de core-banking-api,
 * distribuida como el certificado autofirmado que la contiene (PEM
 * estandar de keytool -exportcert, sin issuer-uri/JWKS porque no hay
 * endpoint de descubrimiento: son 4 servicios cerrados con una llave
 * publica repartida a mano por generate-keystores.ps1).
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${corebanking.jwt.public-key-path}") Resource publicKeyResource)
            throws Exception {
        RSAPublicKey publicKey = loadPublicKey(publicKeyResource);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream in = resource.getInputStream()) {
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(in);
            return (RSAPublicKey) certificate.getPublicKey();
        }
    }
}
