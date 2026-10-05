package com.duoc.corebankingapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Carga el par de llaves RS256 dedicado a firmar JWT (separado del keystore
 * TLS) una sola vez al arrancar. La llave privada nunca sale de este
 * servicio; la llave publica correspondiente se exporta a PEM con
 * generate-keystores.ps1 y se distribuye a los 3 BFF para validar firmas.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    @Bean
    public PrivateKey jwtSigningKey(JwtProperties props) throws Exception {
        KeyStore keyStore = openKeystore(props);
        Key key = keyStore.getKey(props.getKeyAlias(), props.getKeyPassword().toCharArray());
        if (!(key instanceof PrivateKey privateKey)) {
            throw new IllegalStateException(
                    "El alias '" + props.getKeyAlias() + "' del keystore JWT no contiene una llave privada");
        }
        return privateKey;
    }

    @Bean
    public PublicKey jwtVerificationKey(JwtProperties props) throws Exception {
        KeyStore keyStore = openKeystore(props);
        return keyStore.getCertificate(props.getKeyAlias()).getPublicKey();
    }

    private KeyStore openKeystore(JwtProperties props) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        Resource resource = new DefaultResourceLoader().getResource(props.getKeystorePath());
        try (InputStream in = resource.getInputStream()) {
            keyStore.load(in, props.getKeystorePassword().toCharArray());
        }
        return keyStore;
    }
}
