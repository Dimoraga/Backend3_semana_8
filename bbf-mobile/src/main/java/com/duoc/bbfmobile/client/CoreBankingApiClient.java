package com.duoc.bbfmobile.client;

import com.duoc.bbfmobile.client.dto.CoreErrorResponse;
import com.duoc.bbfmobile.client.dto.CoreMovimientoResponse;
import com.duoc.bbfmobile.client.dto.CoreRefreshTokenRequest;
import com.duoc.bbfmobile.client.dto.CoreSaldoResponse;
import com.duoc.bbfmobile.client.dto.CoreTokenResponse;
import com.duoc.bbfmobile.client.dto.CoreTokenUsuarioRequest;
import com.duoc.bbfmobile.exception.CoreBankingApiException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

/** Unico punto de salida hacia core-banking-api (HTTPS + X-Internal-Api-Key ya configurados en el RestClient). */
@Component
@RequiredArgsConstructor
public class CoreBankingApiClient {

    private static final String CANAL = "MOBILE";

    private final RestClient coreBankingRestClient;
    private final ObjectMapper objectMapper;

    public CoreTokenResponse emitirTokenUsuario(String username, String password) {
        return coreBankingRestClient.post()
                .uri("/internal/auth/token/usuario")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoreTokenUsuarioRequest(username, password, CANAL))
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreTokenResponse.class);
    }

    public CoreTokenResponse refrescarToken(String refreshToken) {
        return coreBankingRestClient.post()
                .uri("/internal/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoreRefreshTokenRequest(refreshToken))
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

    public List<CoreMovimientoResponse> obtenerMovimientos(Long cuentaId, int limit) {
        return coreBankingRestClient.get()
                .uri("/internal/cuentas/{id}/movimientos?limit={limit}", cuentaId, limit)
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(new ParameterizedTypeReference<List<CoreMovimientoResponse>>() {
                });
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
