package com.duoc.corebankingapi.security;

import com.duoc.corebankingapi.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emite y valida los JWT RS256 propios de este servicio. La firma se hace
 * con la llave privada (nunca sale de core-banking-api); la verificacion
 * (usada solo para validar refresh tokens que vuelven aqui) usa la llave
 * publica del mismo par. Cada BFF valida los access token de forma
 * independiente con su propia copia de la llave publica.
 */
@Component
@RequiredArgsConstructor
public class JwtIssuer {

    private static final String ISSUER = "core-banking-api";

    private final PrivateKey signingKey;
    private final PublicKey verificationKey;
    private final JwtProperties jwtProperties;

    public String issueAccessToken(String subject, String canal, String roles, Long cuentaId) {
        Duration ttl = Duration.ofSeconds(ttlSecondsFor(canal));
        return build(subject, canal, roles, cuentaId, ttl, null);
    }

    public String issueRefreshToken(String subject, Long cuentaId) {
        Duration ttl = Duration.ofSeconds(jwtProperties.getTtlMobileRefreshSeconds());
        return build(subject, "MOBILE", "ROLE_CLIENTE", cuentaId, ttl, "refresh");
    }

    public long ttlSecondsFor(String canal) {
        return switch (canal) {
            case "WEB" -> jwtProperties.getTtlWebSeconds();
            case "MOBILE" -> jwtProperties.getTtlMobileSeconds();
            case "CAJERO" -> jwtProperties.getTtlCajeroSeconds();
            default -> throw new IllegalArgumentException("Canal desconocido: " + canal);
        };
    }

    /** Valida un refresh token propio (firma, expiracion, type=refresh, canal=MOBILE). */
    public Claims parseRefreshToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(verificationKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Refresh token invalido o expirado", e);
        }
        if (!"refresh".equals(claims.get("type", String.class)) || !"MOBILE".equals(claims.get("canal", String.class))) {
            throw new IllegalArgumentException("El token entregado no es un refresh token de mobile valido");
        }
        return claims;
    }

    private String build(String subject, String canal, String roles, Long cuentaId, Duration ttl, String type) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuer(ISSUER)
                .claim("canal", canal)
                .claim("roles", roles)
                .claim("cuentaId", cuentaId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (type != null) {
            builder.claim("type", type);
        }
        return builder.signWith(signingKey, Jwts.SIG.RS256).compact();
    }
}
