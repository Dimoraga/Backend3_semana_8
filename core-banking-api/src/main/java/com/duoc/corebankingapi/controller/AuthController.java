package com.duoc.corebankingapi.controller;

import com.duoc.corebankingapi.dto.RefreshTokenRequest;
import com.duoc.corebankingapi.dto.TokenResponse;
import com.duoc.corebankingapi.dto.TokenTarjetaRequest;
import com.duoc.corebankingapi.dto.TokenUsuarioRequest;
import com.duoc.corebankingapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Emisor de tokens. Solo lo llaman los 3 BFF (protegido por
 * InternalApiKeyFilter), nunca un cliente final directamente: cada BFF ya
 * valido las credenciales especificas de su canal antes de pedir aqui el
 * JWT correspondiente.
 */
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/token/usuario")
    public TokenResponse tokenUsuario(@Valid @RequestBody TokenUsuarioRequest request) {
        return authService.autenticarUsuario(request.username(), request.password(), request.canal());
    }

    @PostMapping("/token/tarjeta")
    public TokenResponse tokenTarjeta(@Valid @RequestBody TokenTarjetaRequest request) {
        return authService.autenticarTarjeta(request.numeroTarjeta(), request.pin());
    }

    @PostMapping("/token/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refrescarTokenMobile(request.refreshToken());
    }
}
