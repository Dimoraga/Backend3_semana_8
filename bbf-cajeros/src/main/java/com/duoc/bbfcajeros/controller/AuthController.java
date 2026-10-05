package com.duoc.bbfcajeros.controller;

import com.duoc.bbfcajeros.client.CoreBankingApiClient;
import com.duoc.bbfcajeros.client.dto.CoreTokenResponse;
import com.duoc.bbfcajeros.dto.LoginRequest;
import com.duoc.bbfcajeros.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login especifico del canal cajero: numero de tarjeta + PIN (no usuario/clave como web/mobile). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CoreBankingApiClient coreBankingApiClient;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        CoreTokenResponse token = coreBankingApiClient.emitirTokenTarjeta(request.numeroTarjeta(), request.pin());
        return new LoginResponse(token.token(), token.expiresInSeconds(), token.cuentaId());
    }
}
