package com.duoc.corebankingapi.service;

import com.duoc.corebankingapi.dto.TokenResponse;
import com.duoc.corebankingapi.exception.CredencialesInvalidasException;
import com.duoc.corebankingapi.model.Tarjeta;
import com.duoc.corebankingapi.model.Usuario;
import com.duoc.corebankingapi.repository.TarjetaRepository;
import com.duoc.corebankingapi.repository.UsuarioRepository;
import com.duoc.corebankingapi.security.JwtIssuer;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CANAL_CAJERO = "CAJERO";

    private final UsuarioRepository usuarioRepository;
    private final TarjetaRepository tarjetaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    @Transactional(readOnly = true)
    public TokenResponse autenticarUsuario(String username, String rawPassword, String canal) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        Long cuentaId = usuario.getCuenta().getId();
        String token = jwtIssuer.issueAccessToken(usuario.getUsername(), canal, usuario.getRoles(), cuentaId);
        String refreshToken = "MOBILE".equals(canal)
                ? jwtIssuer.issueRefreshToken(usuario.getUsername(), cuentaId)
                : null;

        return new TokenResponse(token, refreshToken, jwtIssuer.ttlSecondsFor(canal), canal, usuario.getRoles(), cuentaId);
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticarTarjeta(String numeroTarjeta, String pin) {
        Tarjeta tarjeta = tarjetaRepository.findByNumeroTarjetaAndActivaTrue(numeroTarjeta)
                .filter(t -> passwordEncoder.matches(pin, t.getPinHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        Long cuentaId = tarjeta.getCuenta().getId();
        String roles = "ROLE_CAJERO";
        String token = jwtIssuer.issueAccessToken(tarjeta.getNumeroTarjeta(), CANAL_CAJERO, roles, cuentaId);

        return new TokenResponse(token, null, jwtIssuer.ttlSecondsFor(CANAL_CAJERO), CANAL_CAJERO, roles, cuentaId);
    }

    public TokenResponse refrescarTokenMobile(String refreshToken) {
        Claims claims;
        try {
            claims = jwtIssuer.parseRefreshToken(refreshToken);
        } catch (IllegalArgumentException e) {
            throw new CredencialesInvalidasException();
        }
        String subject = claims.getSubject();
        Long cuentaId = claims.get("cuentaId", Long.class);
        String roles = claims.get("roles", String.class);

        String nuevoAccessToken = jwtIssuer.issueAccessToken(subject, "MOBILE", roles, cuentaId);
        return new TokenResponse(nuevoAccessToken, refreshToken, jwtIssuer.ttlSecondsFor("MOBILE"), "MOBILE", roles, cuentaId);
    }
}
