package com.duoc.bbfweb.controller;

import com.duoc.bbfweb.client.CoreBankingApiClient;
import com.duoc.bbfweb.client.dto.CoreTokenResponse;
import com.duoc.bbfweb.dto.LoginRequest;
import com.duoc.bbfweb.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login del canal web: usuario/clave, sin deviceId (mobile) ni tarjeta+PIN (cajero). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CoreBankingApiClient coreBankingApiClient;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        CoreTokenResponse token = coreBankingApiClient.emitirTokenUsuario(request.usuario(), request.password());
        return new LoginResponse(token.token(), token.expiresInSeconds());
    }
}
