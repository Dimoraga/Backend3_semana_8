# Semana 8 — Backend for Frontend (BFF) + OAuth2 + Resiliencia

Implementación del patrón **Backend for Frontend** (BFF) sobre el proyecto del banco XYZ que se viene trabajando en el curso. Sobre la base de la semana 5 (BFF con HTTPS y JWT por canal), esta versión agrega:

- **`auth-server`**: un Authorization Server OAuth2 (Spring Authorization Server) que emite tokens `client_credentials` para la comunicación **servicio a servicio** entre los BFF y `core-banking-api`.
- **Tolerancia a fallos con Resilience4j** en los 3 BFF (Circuit Breaker, Retry y Bulkhead, más timeouts), con respuesta controlada **503 + `Retry-After`** cuando el core no está disponible.

## Módulos

| Módulo | Puerto | Rol |
|---|---|---|
| `auth-server` | 9000 (HTTP) | Authorization Server OAuth2. Registra un cliente por BFF y emite JWT `client_credentials` con scopes `core.read` / `core.write`. |
| `core-banking-api` | 8443 (HTTPS) | Único servicio con acceso a MySQL. Emite los JWT de **usuario final** (RS256) y expone `/internal/**`, protegido como OAuth2 Resource Server. |
| `bbf-web` | 8444 (HTTPS) | BFF para navegador: login usuario/clave, dashboard compuesto. |
| `bbf-mobile` | 8445 (HTTPS) | BFF para app móvil: login con `deviceId`, refresh token, payloads livianos. |
| `bbf-cajeros` | 8446 (HTTPS) | BFF para cajeros automáticos: login tarjeta/PIN, saldo y retiro. |
| `bankbatchprocessor` | — | Proyecto Spring Batch de la primera sumativa (repositorio git embebido). Genera las tablas de reporting que `core-banking-api` lee. |

Todos usan **Java 21** y **Spring Boot 4.1.1** con Maven Wrapper.

## Arquitectura

```
                                    ┌──────────────── auth-server (9000) ────────────────┐
                                    │  POST /oauth2/token (client_credentials)           │
                                    │  GET  /oauth2/jwks                                 │
                                    └───────▲────────────────────────────────▲───────────┘
                                            │ client-id/secret               │ JWKS (valida firma)
         HTTPS + JWT usuario                │                                │
Navegador ──▶ bbf-web     (8444) ─┐         │                                │
App móvil ──▶ bbf-mobile  (8445) ─┼─────────┘   HTTPS + Bearer (token        │
Cajero    ──▶ bbf-cajeros (8446) ─┘   ─────────  client_credentials) ──▶ core-banking-api (8443) ──▶ MySQL springbatch_db
                                      RestClient + truststore + Resilience4j
```

Hay **dos tipos de token** con responsabilidades distintas:

1. **JWT de usuario final (por canal)** — los firma `core-banking-api` con su llave privada RS256 al hacer login (vía el BFF). Cada BFF lo valida **localmente** con el certificado público (`keys/core-signing-cert.pem`) y exige que el claim `canal` coincida con el suyo: un token `MOBILE` usado contra `bbf-cajeros` recibe **403** aunque la firma sea válida. Además, cada endpoint verifica que la cuenta del path coincida con el claim `cuentaId` del token.
2. **Token de servicio (OAuth2 `client_credentials`)** — cada BFF lo obtiene de `auth-server` con su `client-id`/`client-secret` (Spring Security OAuth2 Client, cacheado y renovado automáticamente) y lo envía como `Authorization: Bearer` en cada llamada a `core-banking-api`. El core lo valida contra el JWKS del `auth-server` y autoriza por scope:
   - `POST /internal/cuentas/*/retiros` → requiere `SCOPE_core.write` (solo `bbf-cajeros` lo tiene).
   - Resto de `/internal/**` → requiere `SCOPE_core.read`.
   - Cualquier otra ruta → denegada.

> Esto reemplaza al antiguo secreto compartido `X-Internal-Api-Key` de la semana 5, que fue eliminado del código.

## Tolerancia a fallos (Resilience4j)

Los 3 BFF comparten la misma configuración (`config/ResilienceConfig.java`), aplicada como interceptor del `RestClient` hacia el core (`config/CoreBankingClientConfig.java`):

| Mecanismo | Configuración |
|---|---|
| Timeouts | conexión 2 s, lectura 3 s |
| Bulkhead | máx. 10 llamadas concurrentes, espera máx. 200 ms |
| Circuit Breaker | ventana de 10 llamadas, mínimo 5, abre con ≥ 50 % de fallos, 15 s abierto, 3 llamadas de prueba en half-open |
| Retry | 3 intentos con backoff exponencial (300 ms × 2). **Solo en `GET`**: login y retiros (`POST`) nunca se reintentan para no duplicar operaciones |

Solo los errores **5xx** o de red cuentan como fallo; los **4xx** (errores de negocio, como saldo insuficiente) pasan intactos al cliente. Cuando el core no responde o el circuito está abierto, el BFF responde:

```http
HTTP/1.1 503
Retry-After: 15

{"status":503,"message":"El servicio bancario no esta disponible temporalmente. Intente nuevamente en unos segundos.", ...}
```

La carpeta [`evidencia/`](evidencia/) contiene los logs de una ejecución de prueba y la respuesta 503 capturada ([`c12-respuesta-503.txt`](evidencia/c12-respuesta-503.txt)).

## Por qué existe `core-banking-api` y no se reutilizan directamente las tablas de `bankbatchprocessor`

Las 5 tablas que genera `bankbatchprocessor` (`transacciones`, `cuentas_intereses`, `movimientos_cuenta_anual`, `estados_cuenta_anual`, `resumen_transacciones_diarias`) son **salida de un batch/reporting**, no un libro mayor transaccional: no tienen relaciones entre sí, `transacciones` ni siquiera tiene `cuentaId`, y el batch inserta filas nuevas cada vez que corre sin unicidad por cuenta. No son seguras para mutar en vivo (ej. un retiro de cajero).

Por eso `core-banking-api` agrega un dominio propio y mínimo (`cuentas`, `movimientos_cuenta`, `usuarios`, `tarjetas`, `retiro_idempotencia`) en la **misma base de datos** `springbatch_db`, y expone las 5 tablas originales **solo de lectura** para los reportes del dashboard de `bbf-web`. Las cuentas demo reutilizan `cuentaId` reales de `intereses.csv`/`cuentas_anuales.csv` (105, 110, 118) para que el dashboard muestre saldo en vivo junto al historial real de esa cuenta.

## Cómo correrlo

### 1. Requisitos
- Java 21 y MySQL corriendo en `localhost:3306` con la base `springbatch_db` (la misma de `bankbatchprocessor`).
- Un JDK con `keytool` (o `JAVA_HOME` apuntando a uno) para generar los certificados.

### 2. Generar certificados y llaves (una sola vez)
Desde la raíz del proyecto (`Semana_8/`):
```powershell
.\generate-keystores.ps1
```
Genera, para `core-banking-api` y los 3 BFF, un keystore TLS autofirmado; para los 3 BFF, un truststore que confía en el certificado de `core-banking-api`; y el par de llaves RS256 con que `core-banking-api` firma los JWT de usuario (su certificado se copia a los 3 BFF). Es idempotente — no regenera lo que ya existe (usar `-Force` para forzarlo). Los `.p12` quedan gitignored.

> `auth-server` no necesita este paso: corre en HTTP y genera su propia llave de firma en memoria al arrancar.

### 3. Completar `application-local.properties` de cada servicio (gitignored)
El script imprime las contraseñas al final. Hay que copiarlas a mano en:
- `core-banking-api/src/main/resources/application-local.properties`: `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `server.ssl.key-store-password`, `corebanking.jwt.keystore-password`, `corebanking.jwt.key-password`.
- `bbf-web` / `bbf-mobile` / `bbf-cajeros`, cada uno en su propio `application-local.properties`:
  - `server.ssl.key-store-password`
  - `corebanking.client.trust-store-password`
  - `spring.security.oauth2.client.registration.core.client-secret` — **debe coincidir** con el secreto del cliente en `auth-server` (por defecto `dev-web-secret`, `dev-mobile-secret` y `dev-cajeros-secret`; se pueden cambiar con las variables de entorno `BBF_WEB_SECRET`, `BBF_MOBILE_SECRET`, `BBF_CAJEROS_SECRET`).

Variables de entorno opcionales para las URLs del `auth-server`: `AUTH_ISSUER`, `AUTH_JWK_URI` (core) y `AUTH_TOKEN_URI` (BFF). Por defecto apuntan a `http://localhost:9000`.

### 4. Levantar los servicios (orden: auth-server → core → BFF)
Cada uno en su propia terminal:
```powershell
cd auth-server;      .\mvnw.cmd spring-boot:run
cd core-banking-api; .\mvnw.cmd spring-boot:run
cd bbf-web;          .\mvnw.cmd spring-boot:run
cd bbf-mobile;       .\mvnw.cmd spring-boot:run
cd bbf-cajeros;      .\mvnw.cmd spring-boot:run
```
`core-banking-api` crea las tablas nuevas (`ddl-auto=update`) y siembra 3 cuentas demo si están vacías. Para que el dashboard muestre historial, las tablas de reporting deben haber sido pobladas antes ejecutando `bankbatchprocessor`.

### 5. Credenciales de demo (sembradas por `DemoDataSeeder`)

| Cuenta | Titular | Usuario web/mobile | Clave | Tarjeta (cajero) | PIN |
|---|---|---|---|---|---|
| 105 | Diana Prince | `usuario105` | `demo1234` | `4000-1234-5678-0105` | `1234` |
| 110 | Charlie Green | `usuario110` | `demo1234` | `4000-1234-5678-0110` | `2345` |
| 118 | John Doe | `usuario118` | `demo1234` | `4000-1234-5678-0118` | `3456` |

En la raíz hay cuerpos JSON listos para pruebas con `curl -d @archivo`: `login.json` (web), `login-mobile.json`, `login-cajero.json` y `retiro.json`.

## Endpoints por canal (mismo dato, distinto shape)

| | `bbf-web` (8444) | `bbf-mobile` (8445) | `bbf-cajeros` (8446) |
|---|---|---|---|
| Login | `POST /auth/login` {usuario, password} → `{token, expiresInSeconds}` | `POST /auth/login` {usuario, password, deviceId} → `{accessToken, refreshToken, expiresInSeconds}` + `POST /auth/refresh` | `POST /auth/login` {numeroTarjeta, pin} → `{token, expiresInSeconds, cuentaId}` |
| Duración del token | 30 min | 15 min (refresh: 7 días) | 5 min |
| Saldo | `GET /cuentas/{id}/saldo` → cuentaId, numeroCuenta, titular, tipo, saldo, moneda, fecha | `GET /cuentas/{id}/saldo` → cuentaId, saldo, moneda | `GET /cuentas/{id}/saldo` → solo `{saldo}` |
| Movimientos | `GET /cuentas/{id}/movimientos` (hasta 50, todos los campos) | `GET /cuentas/{id}/movimientos` (hasta 10, solo tipo/monto/fecha) | — |
| Extra | `GET /cuentas/{id}/dashboard` (saldo + movimientos + estado anual + intereses + resumen del sistema en un solo response) y `GET /cuentas/{id}/estado-anual` | — | `POST /cuentas/{id}/retiros` {monto} (+ header opcional `Idempotency-Key`) |

Endpoints internos de `core-banking-api` (solo para los BFF): `/internal/auth/token/{usuario|tarjeta|refresh}`, `/internal/cuentas/{id}/{saldo|movimientos|retiros}` y `/internal/reportes/...` (estado anual, intereses, movimientos anuales, resumen diario).

## Qué es real y qué está simplificado a propósito

| Aspecto | Estado |
|---|---|
| HTTPS con certificado propio por servicio (core y BFF) | Real |
| Firma/validación JWT de usuario RS256, llave privada solo en `core-banking-api` | Real |
| Rechazo de token de otro canal (claim `canal`) y de otra cuenta (claim `cuentaId`) | Real |
| Autenticación servicio a servicio con OAuth2 `client_credentials` y autorización por scope | Real |
| Circuit Breaker, Retry (solo GET), Bulkhead y timeouts hacia el core | Real |
| Retiro con `SELECT ... FOR UPDATE` (lock pesimista) + `@Version` de respaldo | Real |
| Idempotencia de retiros (`Idempotency-Key`) | Real, pero liviana: sin TTL ni alcance por cuenta |
| `auth-server` | Simplificado: HTTP (sin TLS), clientes y secretos `{noop}` en `application.properties`, llave de firma en memoria (se regenera al reiniciar) |
| Login de usuario final vía OIDC (Keycloak, Authorization Code) | No usado — los JWT de usuario los sigue emitiendo `core-banking-api` |
| Revocación de refresh token | No implementada — solo expira (7 días) |
| Rate limiting | No implementado (el Bulkhead limita concurrencia, no tasa) |
| Acceso solo-lectura a las tablas de `bankbatchprocessor` | Convención de código (sin `save()`/`delete()`), no un grant de BD |
| Bloqueo de tarjeta tras PIN incorrecto repetido | No implementado (campo `intentosFallidos` reservado a futuro) |

## Verificación rápida (PowerShell 7+ con `-SkipCertificateCheck`, o `curl -k`)

```powershell
# Login cajero, saldo y retiro
$login = Invoke-RestMethod -Method Post -Uri https://localhost:8446/auth/login -SkipCertificateCheck `
  -ContentType application/json -Body '{"numeroTarjeta":"4000-1234-5678-0110","pin":"2345"}'
Invoke-RestMethod -Uri https://localhost:8446/cuentas/110/saldo -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($login.token)"}
Invoke-RestMethod -Method Post -Uri https://localhost:8446/cuentas/110/retiros -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($login.token)"; "Idempotency-Key" = "demo-1"} `
  -ContentType application/json -Body '{"monto":10000}'

# Canal cruzado: un token de mobile contra cajeros debe dar 403
$mobile = Invoke-RestMethod -Method Post -Uri https://localhost:8445/auth/login -SkipCertificateCheck `
  -ContentType application/json -Body '{"usuario":"usuario105","password":"demo1234","deviceId":"dev-1"}'
Invoke-RestMethod -Uri https://localhost:8446/cuentas/105/saldo -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($mobile.accessToken)"}   # -> 403

# Token de servicio emitido por auth-server (lo que usan los BFF internamente)
curl.exe -u bbf-cajeros:dev-cajeros-secret -d "grant_type=client_credentials&scope=core.read core.write" `
  http://localhost:9000/oauth2/token

# Resiliencia: detener core-banking-api y repetir una consulta de saldo -> 503 con Retry-After: 15
Invoke-WebRequest -Uri https://localhost:8446/cuentas/110/saldo -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($login.token)"} -SkipHttpErrorCheck
```
