package com.duoc.bbfcajeros.client;

import com.duoc.bbfcajeros.client.dto.CoreErrorResponse;
import com.duoc.bbfcajeros.client.dto.CoreRetiroRequest;
import com.duoc.bbfcajeros.client.dto.CoreRetiroResultado;
import com.duoc.bbfcajeros.client.dto.CoreSaldoResponse;
import com.duoc.bbfcajeros.client.dto.CoreTokenResponse;
import com.duoc.bbfcajeros.client.dto.CoreTokenTarjetaRequest;
import com.duoc.bbfcajeros.exception.CoreBankingApiException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/** Unico punto de salida hacia core-banking-api (HTTPS + X-Internal-Api-Key ya configurados en el RestClient). */
@Component
@RequiredArgsConstructor
public class CoreBankingApiClient {

    private final RestClient coreBankingRestClient;
    private final ObjectMapper objectMapper;

    public CoreTokenResponse emitirTokenTarjeta(String numeroTarjeta, String pin) {
        return coreBankingRestClient.post()
                .uri("/internal/auth/token/tarjeta")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoreTokenTarjetaRequest(numeroTarjeta, pin))
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreTokenResponse.class);
    }

    public CoreSaldoResponse obtenerSaldo(Long cuentaId) {
        return coreBankingRestClient.get()
                .uri("/internal/cuentas/{id}/saldo", cuentaId)
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreSaldoResponse.class);
    }

    public CoreRetiroResultado retirar(Long cuentaId, CoreRetiroRequest request) {
        return coreBankingRestClient.post()
                .uri("/internal/cuentas/{id}/retiros", cuentaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreRetiroResultado.class);
    }

    private void propagarError(HttpRequest request, ClientHttpResponse response) throws IOException {
        String mensaje;
        try {
            CoreErrorResponse error = objectMapper.readValue(response.getBody(), CoreErrorResponse.class);
            mensaje = error.message();
        } catch (Exception e) {
            mensaje = "Error al llamar a core-banking-api (" + response.getStatusCode() + ")";
        }
        throw new CoreBankingApiException(response.getStatusCode(), mensaje);
    }
}
