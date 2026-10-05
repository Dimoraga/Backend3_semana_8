# Semana 5 — Backend for Frontend (BFF)

Implementación del patrón **Backend for Frontend** (BFF) sobre el proyecto en el cual se viene trabajando a lo largo de la semana referente al banco XYZ. Se agregaron 4 servicios Spring Boot nuevos:


## Arquitectura

```
                         HTTPS                                    HTTPS
  Navegador  ──login usuario/clave──▶  bbf-web (8444)  ─┐
  App móvil  ──login usuario/clave/deviceId──▶ bbf-mobile (8445) ─┼─ X-Internal-Api-Key ──▶ core-banking-api (8443) ──▶ MySQL springbatch_db
  Cajero     ──login tarjeta/PIN──▶  bbf-cajeros (8446) ─┘         (RestClient + truststore)
```

- **core-banking-api** es el único servicio con acceso a la base de datos. Nunca lo llama un cliente final: solo los 3 BFF, autenticados con un secreto compartido (`X-Internal-Api-Key`), distinto de los JWT de usuario.
- Cada BFF valida los JWT **localmente** con la llave pública RS256 de `core-banking-api` (sin llamar a ningún servicio para validar) y además exige que el claim `canal` del token coincida con el suyo — un token de `MOBILE` recibe **403** si se usa contra `bbf-cajeros`, aunque la firma sea válida. Así se cumple la autenticación/autorización específica por canal.
- Los 4 servicios corren sobre **HTTPS** con certificados autofirmados propios (uno por servicio).

## Por qué existe `core-banking-api` y no se reutilizan directamente las tablas de `bankbatchprocessor`

Las 5 tablas que genera `bankbatchprocessor` (`transacciones`, `cuentas_intereses`, `movimientos_cuenta_anual`, `estados_cuenta_anual`, `resumen_transacciones_diarias`) son **salida de un batch/reporting**, no un libro mayor transaccional: no tienen relaciones entre sí, `transacciones` ni siquiera tiene `cuentaId`, y el batch inserta filas nuevas cada vez que corre sin unicidad por cuenta. No son seguras para mutar en vivo (ej. un retiro de cajero).

Por eso `core-banking-api` agrega un dominio propio y mínimo (`cuentas`, `movimientos_cuenta`, `usuarios`, `tarjetas`, `retiro_idempotencia`) en la **misma base de datos** `springbatch_db`, y expone las 5 tablas originales **solo de lectura** para los reportes del dashboard de `bbf-web`. Las cuentas demo reutilizan `cuentaId` reales de `intereses.csv`/`cuentas_anuales.csv` (105, 110, 118) para que el dashboard muestre saldo en vivo junto al historial real de esa cuenta.

## Cómo correrlo

### 1. Requisitos
- Java 21, MySQL corriendo en `localhost:3306` (mismo que usa `bankbatchprocessor`).
- Un JDK con `keytool` (o `JAVA_HOME` apuntando a uno) para generar los certificados.

### 2. Generar certificados y llaves (una sola vez)
Desde `Semana_5/`:
```powershell
.\generate-keystores.ps1
```
Genera, para cada uno de los 4 servicios, un keystore TLS autofirmado; para los 3 BFF, un truststore que confía en el certificado de `core-banking-api`; y el par de llaves RS256 que `core-banking-api` usa para firmar JWT (con su certificado copiado a los 3 BFF para validar la firma). Es idempotente — no regenera lo que ya existe (usar `-Force` para forzarlo).

### 3. Completar `application-local.properties` de cada servicio (gitignored)
El script imprime las contraseñas generadas al final. Hay que copiarlas a mano en:
- `core-banking-api/src/main/resources/application-local.properties`: `spring.datasource.password` (tu MySQL local), `server.ssl.key-store-password`, `corebanking.jwt.keystore-password`, `corebanking.jwt.key-password`, `corebanking.security.internal-api-key`.
- `bbf-web` / `bbf-mobile` / `bbf-cajeros` — cada uno con su propio `application-local.properties`: `server.ssl.key-store-password`, `corebanking.client.trust-store-password`, `corebanking.client.internal-api-key` (**debe ser igual** al de `core-banking-api`).

### 4. Levantar los servicios (orden: core primero)
```powershell
cd core-banking-api; .\mvnw.cmd spring-boot:run
cd bbf-web; .\mvnw.cmd spring-boot:run
cd bbf-mobile; .\mvnw.cmd spring-boot:run
cd bbf-cajeros; .\mvnw.cmd spring-boot:run
```
`core-banking-api` crea las tablas nuevas (`ddl-auto=update`) y siembra 3 cuentas demo si están vacías.

### 5. Credenciales de demo (sembradas por `DemoDataSeeder`)

| Cuenta | Titular | Usuario web/mobile | Clave | Tarjeta (cajero) | PIN |
|---|---|---|---|---|---|
| 105 | Diana Prince | `usuario105` | `demo1234` | `4000-1234-5678-0105` | `1234` |
| 110 | Charlie Green | `usuario110` | `demo1234` | `4000-1234-5678-0110` | `2345` |
| 118 | John Doe | `usuario118` | `demo1234` | `4000-1234-5678-0118` | `3456` |

## Endpoints por canal (mismo dato, distinto shape)

| | `bbf-web` (8444) | `bbf-mobile` (8445) | `bbf-cajeros` (8446) |
|---|---|---|---|
| Login | `POST /auth/login` {usuario, password} | `POST /auth/login` {usuario, password, deviceId} + `POST /auth/refresh` | `POST /auth/login` {numeroTarjeta, pin} |
| Saldo | `GET /cuentas/{id}/saldo` → cuentaId, numeroCuenta, titular, tipo, saldo, moneda, fecha | `GET /cuentas/{id}/saldo` → cuentaId, saldo, moneda | `GET /cuentas/{id}/saldo` → solo `{saldo}` |
| Movimientos | `GET /cuentas/{id}/movimientos` (hasta 50, todos los campos) | `GET /cuentas/{id}/movimientos` (hasta 10, solo tipo/monto/fecha) | — |
| Extra | `GET /cuentas/{id}/dashboard` (compone saldo + movimientos + estado anual + intereses + resumen del sistema en un solo response) y `GET /cuentas/{id}/estado-anual` | — | `POST /cuentas/{id}/retiros` {monto} (+ header opcional `Idempotency-Key`) |

Los 3 verifican que la cuenta del path coincida con la `cuentaId` del JWT (403 si no).

## Qué es real y qué está simplificado a propósito

| Aspecto | Estado |
|---|---|
| HTTPS con certificado propio por servicio | Real |
| Firma/validación JWT RS256, llave privada solo en `core-banking-api` | Real |
| Rechazo de token de otro canal (claim `canal`) | Real |
| Retiro con `SELECT ... FOR UPDATE` (lock pesimista) + `@Version` de respaldo | Real |
| Idempotencia de retiros (`Idempotency-Key`) | Real, pero liviana: sin TTL ni alcance por cuenta |
| IdP externo (Keycloak/OIDC) | No usado — decisión explícita: JWT propio |
| Revocación de refresh token | No implementada — solo expira (7 días) |
| Rate limiting | No implementado más allá de la dedup de idempotencia |
| Acceso solo-lectura a las tablas de `bankbatchprocessor` | Convención de código (sin `save()`/`delete()`), no un grant de BD |
| Bloqueo de tarjeta tras PIN incorrecto repetido | No implementado (campo `intentosFallidos` reservado a futuro) |

## Verificación rápida (PowerShell 7+ con `-SkipCertificateCheck`, o `curl -k`)

```powershell
# Login cajero y retiro
$login = Invoke-RestMethod -Method Post -Uri https://localhost:8446/auth/login -SkipCertificateCheck `
  -ContentType application/json -Body '{"numeroTarjeta":"4000-1234-5678-0110","pin":"2345"}'
Invoke-RestMethod -Uri https://localhost:8446/cuentas/110/saldo -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($login.token)"}
Invoke-RestMethod -Method Post -Uri https://localhost:8446/cuentas/110/retiros -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($login.token)"; "Idempotency-Key" = "demo-1"} `
  -ContentType application/json -Body '{"monto":10000}'

# Prueba de canal cruzado: un token de mobile contra cajeros debe dar 403
$mobile = Invoke-RestMethod -Method Post -Uri https://localhost:8445/auth/login -SkipCertificateCheck `
  -ContentType application/json -Body '{"usuario":"usuario105","password":"demo1234","deviceId":"dev-1"}'
Invoke-RestMethod -Uri https://localhost:8446/cuentas/105/saldo -SkipCertificateCheck `
  -Headers @{Authorization = "Bearer $($mobile.accessToken)"}   # -> 403
```
