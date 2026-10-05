package com.duoc.bbfweb.config;

import com.duoc.bbfweb.exception.ServicioNoDisponibleException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * RestClient hacia core-banking-api con HTTPS real (truststore que confia
 * en su certificado autofirmado), token OAuth2 client_credentials del
 * auth-server en cada request y proteccion Resilience4j.
 */
@Configuration
public class CoreBankingClientConfig {

    @Bean
    public RestClient coreBankingRestClient(
            @Value("${corebanking.client.base-url}") String baseUrl,
            @Value("${corebanking.client.trust-store}") Resource trustStoreResource,
            @Value("${corebanking.client.trust-store-password:}") String trustStorePassword,
            OAuth2AuthorizedClientManager authorizedClientManager,
            CircuitBreaker circuitBreaker, Retry retry, Bulkhead bulkhead) throws Exception {

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2));
        // En la red Docker (perfil docker) core va por HTTP: el truststore solo aplica a https://
        if (baseUrl.startsWith("https://")) {
            httpClientBuilder.sslContext(buildSslContext(trustStoreResource, trustStorePassword));
        }
        HttpClient httpClient = httpClientBuilder.build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(resilienceInterceptor(circuitBreaker, retry, bulkhead))
                .requestInterceptor((request, body, execution) -> {
                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                            .withClientRegistrationId("core")
                            .principal("bbf-web")
                            .build();
                    OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
                    if (client == null) {
                        throw new IllegalStateException("No se pudo obtener token del auth-server");
                    }
                    request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    private ClientHttpRequestInterceptor resilienceInterceptor(CircuitBreaker cb, Retry retry, Bulkhead bulkhead) {
        return (request, body, execution) -> {
            Supplier<ClientHttpResponse> llamada = () -> {
                try {
                    ClientHttpResponse response = execution.execute(request, body);
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is5xxServerError()) {
                        response.close();
                        throw new ServicioNoDisponibleException("core-banking-api respondio " + status.value());
                    }
                    return response; // 2xx y 4xx (errores de negocio) pasan intactos
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            Supplier<ClientHttpResponse> protegida = Bulkhead.decorateSupplier(bulkhead, llamada);
            protegida = CircuitBreaker.decorateSupplier(cb, protegida);
            if (HttpMethod.GET.equals(request.getMethod())) { // POST (login, retiro) nunca se reintenta
                protegida = Retry.decorateSupplier(retry, protegida);
            }

            try {
                return protegida.get();
            } catch (ServicioNoDisponibleException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ServicioNoDisponibleException("core-banking-api no disponible", e);
            }
        };
    }

    private SSLContext buildSslContext(Resource trustStoreResource, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = trustStoreResource.getInputStream()) {
            trustStore.load(in, password.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
}
