package com.duoc.bbfmobile.controller;

import com.duoc.bbfmobile.client.CoreBankingApiClient;
import com.duoc.bbfmobile.client.dto.CoreTokenResponse;
import com.duoc.bbfmobile.dto.LoginRequest;
import com.duoc.bbfmobile.dto.LoginResponse;
import com.duoc.bbfmobile.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login del canal mobile: usuario/clave + deviceId (a diferencia de web,
 * que no pide deviceId, y de cajero, que usa tarjeta+PIN). Unico canal con
 * refresh token, por la naturaleza de sesiones mas largas en apps moviles.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CoreBankingApiClient coreBankingApiClient;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        CoreTokenResponse token = coreBankingApiClient.emitirTokenUsuario(request.usuario(), request.password());
        return new LoginResponse(token.token(), token.refreshToken(), token.expiresInSeconds());
    }

    @PostMapping("/refresh")
    public LoginResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        CoreTokenResponse token = coreBankingApiClient.refrescarToken(request.refreshToken());
        return new LoginResponse(token.token(), token.refreshToken(), token.expiresInSeconds());
    }
}
