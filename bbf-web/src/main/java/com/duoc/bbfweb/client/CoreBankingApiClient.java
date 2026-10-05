package com.duoc.bbfweb.client;

import com.duoc.bbfweb.client.dto.CoreErrorResponse;
import com.duoc.bbfweb.client.dto.CoreEstadoAnualResponse;
import com.duoc.bbfweb.client.dto.CoreInteresResponse;
import com.duoc.bbfweb.client.dto.CoreMovimientoResponse;
import com.duoc.bbfweb.client.dto.CoreResumenDiarioResponse;
import com.duoc.bbfweb.client.dto.CoreSaldoResponse;
import com.duoc.bbfweb.client.dto.CoreTokenResponse;
import com.duoc.bbfweb.client.dto.CoreTokenUsuarioRequest;
import com.duoc.bbfweb.exception.CoreBankingApiException;
import com.duoc.bbfweb.exception.ServicioNoDisponibleException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Unico punto de salida hacia core-banking-api (HTTPS + X-Internal-Api-Key ya configurados en el RestClient). */
@Component
@RequiredArgsConstructor
public class CoreBankingApiClient {

    private static final String CANAL = "WEB";

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

    public Optional<CoreEstadoAnualResponse> obtenerEstadoAnual(Long cuentaId) {
        return obtenerOpcional(() -> coreBankingRestClient.get()
                .uri("/internal/reportes/cuentas/{id}/estado-anual", cuentaId)
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreEstadoAnualResponse.class));
    }

    public Optional<CoreInteresResponse> obtenerIntereses(Long cuentaId) {
        return obtenerOpcional(() -> coreBankingRestClient.get()
                .uri("/internal/reportes/cuentas/{id}/intereses", cuentaId)
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreInteresResponse.class));
    }

    public Optional<CoreResumenDiarioResponse> obtenerResumenDiario() {
        return obtenerOpcional(() -> coreBankingRestClient.get()
                .uri("/internal/reportes/resumen-diario")
                .retrieve()
                .onStatus(status -> status.isError(), this::propagarError)
                .body(CoreResumenDiarioResponse.class));
    }

    /** Los reportes historicos pueden simplemente no existir para una cuenta (404 = vacio, no un error). */
    private <T> Optional<T> obtenerOpcional(java.util.function.Supplier<T> llamada) {
        try {
            return Optional.ofNullable(llamada.get());
        } catch (CoreBankingApiException e) {
            if (e.getStatus().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw e;
        } catch (ServicioNoDisponibleException e) {
            return Optional.empty(); // fallback: el dashboard degrada, sin reportes historicos
        }
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
