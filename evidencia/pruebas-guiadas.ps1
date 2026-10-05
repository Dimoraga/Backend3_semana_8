<#
.SYNOPSIS
    Pruebas guiadas para sacar capturas de evidencia (OAuth2, Docker, docker-compose,
    Resilience4j, Kafka y Batch) contra el stack de docker compose de Semana_8.

.DESCRIPTION
    Ejecuta 7 secciones, una por pantalla. Cada una muestra un titulo, que se prueba,
    los comandos (antes de ejecutarlos) y su resultado, y espera Enter para continuar.
    No muestra secretos: los tokens se guardan en variables y solo se imprimen sus
    primeros 25 caracteres. Si la seccion de Resilience4j detiene core-banking-api,
    el script lo vuelve a iniciar aunque se interrumpa.

.PARAMETER SinPausas
    Ejecuta todo de corrido, sin Clear-Host ni pausas (para verificar el script).

.EXAMPLE
    .\evidencia\pruebas-guiadas.ps1
.EXAMPLE
    .\evidencia\pruebas-guiadas.ps1 -SinPausas
#>
param(
    [switch]$SinPausas
)

$raiz = Split-Path -Parent $PSScriptRoot      # Semana_8 (donde estan docker-compose.yaml y los JSON)
$cuenta = 105
$tmp = Join-Path $env:TEMP "pruebas-guiadas-$PID.json"
$script:numero = 0

# ---------- Utilidades de presentacion ----------

function Start-Seccion([string]$Titulo, [string]$Explicacion) {
    $script:numero++
    if (-not $SinPausas) { Clear-Host }
    $linea = "=" * 78
    Write-Host ""
    Write-Host $linea -ForegroundColor Cyan
    Write-Host ("  EVIDENCIA {0}: {1}" -f $script:numero, $Titulo) -ForegroundColor Cyan
    Write-Host $linea -ForegroundColor Cyan
    Write-Host "  $Explicacion" -ForegroundColor White
    Write-Host ("  Fecha: {0}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss")) -ForegroundColor DarkGray
    Write-Host ""
}

function Show-Comando([string]$Comando) {
    Write-Host "PS> $Comando" -ForegroundColor Yellow
}

function Show-Resultado([string]$Etiqueta, [string]$Codigo, [string]$Esperado) {
    $color = if ($Codigo -eq $Esperado) { "Green" } else { "Red" }
    Write-Host ("    {0} -> HTTP {1}   (esperado: {2})" -f $Etiqueta, $Codigo, $Esperado) -ForegroundColor $color
}

function Show-Token([string]$Nombre, [string]$Token) {
    if ($Token) {
        $corto = $Token.Substring(0, [Math]::Min(25, $Token.Length))
        Write-Host ("    {0} = {1}...  (token completo guardado en variable, no se muestra)" -f $Nombre, $corto) -ForegroundColor Gray
    } else {
        Write-Host "    $Nombre = (no se obtuvo token)" -ForegroundColor Red
    }
}

function Wait-Captura([string]$Mensaje = "Saca tu captura y presiona Enter para continuar") {
    Write-Host ""
    if ($SinPausas) {
        Write-Host "[-SinPausas] $Mensaje (omitido)" -ForegroundColor DarkGray
    } else {
        Read-Host $Mensaje | Out-Null
    }
}

# ---------- Utilidades HTTP (curl.exe; los JSON siempre desde archivo) ----------

# Ejecuta curl.exe guardando el cuerpo en un archivo temporal; devuelve codigo y cuerpo.
function Invoke-Curl([string[]]$Argumentos) {
    if (Test-Path $tmp) { Remove-Item $tmp -Force }
    $codigo = & curl.exe -s -o $tmp -w "%{http_code}" @Argumentos
    $cuerpo = if (Test-Path $tmp) { Get-Content $tmp -Raw } else { "" }
    if (Test-Path $tmp) { Remove-Item $tmp -Force }
    return @{ Codigo = "$codigo"; Cuerpo = "$cuerpo".Trim() }
}

# Login contra un BFF con un archivo JSON; devuelve codigo y token (el token NO se imprime).
function Invoke-Login([int]$Puerto, [string]$Archivo, [string]$CampoToken) {
    $r = Invoke-Curl @("-X", "POST", "http://localhost:$Puerto/auth/login",
        "-H", "Content-Type: application/json", "--data-binary", "@$Archivo")
    $token = $null
    if ($r.Codigo -eq "200") {
        try { $token = ($r.Cuerpo | ConvertFrom-Json).$CampoToken } catch { $token = $null }
    }
    return @{ Codigo = $r.Codigo; Token = $token }
}

function Get-CodigoSaldoWeb([string]$Token) {
    return "$(& curl.exe -s -o NUL -w '%{http_code}' -m 10 "http://localhost:8444/cuentas/$cuenta/saldo" -H "Authorization: Bearer $Token")"
}

function Test-ServicioArriba([string]$Servicio) {
    $arriba = @(docker compose ps --services --status running 2>$null)
    return $arriba -contains $Servicio
}

# ---------- Verificacion inicial ----------

Push-Location $raiz
try {
    Write-Host "Verificando el stack de docker compose en $raiz ..." -ForegroundColor Cyan
    $null = docker compose ps 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: 'docker compose ps' no responde. Inicia Docker Desktop y ejecuta 'docker compose up -d' en $raiz." -ForegroundColor Red
        exit 1
    }
    $necesarios = @("mysql", "kafka", "auth-server", "core-banking-api", "bbf-web", "bbf-cajeros", "notificaciones")
    $arriba = @(docker compose ps --services --status running 2>$null)
    $faltan = @($necesarios | Where-Object { $arriba -notcontains $_ })
    if ($faltan.Count -gt 0) {
        Write-Host ("ERROR: estos servicios no estan Up: {0}" -f ($faltan -join ", ")) -ForegroundColor Red
        Write-Host "Ejecuta 'docker compose up -d', espera 1-2 minutos y vuelve a correr este script." -ForegroundColor Red
        exit 1
    }
    foreach ($archivo in @("login.json", "login-cajero.json", "retiro.json")) {
        if (-not (Test-Path (Join-Path $raiz $archivo))) {
            Write-Host "ERROR: falta $archivo en $raiz" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host ("OK: servicios Up -> {0}" -f ($necesarios -join ", ")) -ForegroundColor Green
    Wait-Captura "Presiona Enter para comenzar"

    # ===================== 1. Docker =====================
    Start-Seccion "Docker - imagenes y servicios de docker compose" `
        "Una imagen por servicio (banco/*:1.0) y todos los contenedores del compose: running/healthy y one-shots con Exited (0)."
    Show-Comando 'docker images --filter "reference=banco/*"'
    docker images --filter "reference=banco/*" --format "table {{.Repository}}:{{.Tag}}`t{{.ID}}`t{{.Size}}`t{{.CreatedSince}}"
    Write-Host ""
    Show-Comando 'docker compose ps -a'
    docker compose ps -a --format "table {{.Service}}`t{{.Image}}`t{{.Status}}`t{{.Ports}}"
    Wait-Captura

    # ===================== 2. OAuth2 - 401 =====================
    Start-Seccion "OAuth2 - 401 sin token" `
        "core-banking-api es un Resource Server: una llamada directa a /internal/** sin token Bearer debe ser rechazada con 401."
    Show-Comando "curl.exe -s -o NUL -w `"%{http_code}`" http://localhost:8443/internal/cuentas/$cuenta/saldo"
    $codigo = "$(& curl.exe -s -o NUL -w '%{http_code}' "http://localhost:8443/internal/cuentas/$cuenta/saldo")"
    Show-Resultado "GET core /internal/cuentas/$cuenta/saldo (sin token)" $codigo "401"
    Wait-Captura

    # ===================== 3. OAuth2 - 200 =====================
    Start-Seccion "OAuth2 - 200 login web + saldo" `
        "bbf-web hace login del usuario y consulta el saldo en core con un token client_credentials (scope core.read) emitido por auth-server."
    Show-Comando 'curl.exe -s -X POST http://localhost:8444/auth/login -H "Content-Type: application/json" --data-binary "@login.json"'
    $loginWeb = Invoke-Login 8444 "login.json" "token"
    Show-Resultado "POST bbf-web /auth/login" $loginWeb.Codigo "200"
    Show-Token '$tokenWeb' $loginWeb.Token
    $tokenWeb = $loginWeb.Token
    Write-Host ""
    Show-Comando "curl.exe -s http://localhost:8444/cuentas/$cuenta/saldo -H `"Authorization: Bearer `$tokenWeb`""
    $saldo = Invoke-Curl @("http://localhost:8444/cuentas/$cuenta/saldo", "-H", "Authorization: Bearer $tokenWeb")
    Show-Resultado "GET bbf-web /cuentas/$cuenta/saldo" $saldo.Codigo "200"
    Write-Host "    Respuesta: $($saldo.Cuerpo)" -ForegroundColor Gray
    Wait-Captura

    # ===================== 4. OAuth2 - 403 =====================
    Start-Seccion "OAuth2 - 403 token de otro canal" `
        "Un token de usuario WEB (firma valida) usado para un retiro en bbf-cajeros: el claim 'canal' no es CAJERO, debe responder 403."
    Show-Comando "curl.exe -s -o NUL -w `"%{http_code}`" -X POST http://localhost:8446/cuentas/$cuenta/retiros -H `"Authorization: Bearer `$tokenWeb`" -H `"Content-Type: application/json`" --data-binary `"@retiro.json`""
    $codigo = "$(& curl.exe -s -o NUL -w '%{http_code}' -X POST "http://localhost:8446/cuentas/$cuenta/retiros" -H "Authorization: Bearer $tokenWeb" -H "Content-Type: application/json" --data-binary "@retiro.json")"
    Show-Resultado "POST bbf-cajeros /cuentas/$cuenta/retiros con token WEB" $codigo "403"
    Wait-Captura

    # ===================== 5. Kafka =====================
    Start-Seccion "Kafka - retiro por cajero y notificacion" `
        "Login de cajero y retiro de 1000 en la cuenta ${cuenta}: core publica el evento en 'retiros-realizados' y el servicio notificaciones lo consume."
    Show-Comando 'curl.exe -s -X POST http://localhost:8446/auth/login -H "Content-Type: application/json" --data-binary "@login-cajero.json"'
    $loginCajero = Invoke-Login 8446 "login-cajero.json" "token"
    Show-Resultado "POST bbf-cajeros /auth/login" $loginCajero.Codigo "200"
    Show-Token '$tokenCajero' $loginCajero.Token
    Write-Host ""
    Show-Comando "curl.exe -s -X POST http://localhost:8446/cuentas/$cuenta/retiros -H `"Authorization: Bearer `$tokenCajero`" -H `"Content-Type: application/json`" --data-binary `"@retiro.json`""
    $retiro = Invoke-Curl @("-X", "POST", "http://localhost:8446/cuentas/$cuenta/retiros",
        "-H", "Authorization: Bearer $($loginCajero.Token)", "-H", "Content-Type: application/json", "--data-binary", "@retiro.json")
    Show-Resultado "POST bbf-cajeros /cuentas/$cuenta/retiros (1000)" $retiro.Codigo "200"
    Write-Host "    Respuesta: $($retiro.Cuerpo)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Esperando 3 segundos a que notificaciones consuma el evento..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
    Show-Comando 'docker compose logs notificaciones --tail 5'
    docker compose logs notificaciones --tail 5 --no-log-prefix
    Wait-Captura

    # ===================== 6. Resilience4j =====================
    Start-Seccion "Resilience4j - circuit breaker con core caido" `
        "Se detiene core-banking-api: bbf-web responde 503 con Retry-After (fallback) y el circuit breaker pasa de CLOSED a OPEN."
    try {
        Show-Comando 'docker compose stop core-banking-api'
        docker compose stop core-banking-api
        Write-Host ""
        Show-Comando "curl.exe -s -o NUL -w `"%{http_code} Retry-After=%header{retry-after}`" http://localhost:8444/cuentas/$cuenta/saldo -H `"Authorization: Bearer `$tokenWeb`"   (x8)"
        for ($i = 1; $i -le 8; $i++) {
            $salida = "$(& curl.exe -s -o NUL -w '%{http_code} Retry-After=%header{retry-after}' -m 10 "http://localhost:8444/cuentas/$cuenta/saldo" -H "Authorization: Bearer $tokenWeb")"
            $codigo = ($salida -split ' ')[0]
            $color = if ($codigo -eq "503") { "Green" } else { "Red" }
            Write-Host ("    Consulta {0}: HTTP {1}   (esperado: 503)" -f $i, $salida) -ForegroundColor $color
        }
        Wait-Captura

        if (-not $SinPausas) { Clear-Host }
        Write-Host ""
        Write-Host ("  EVIDENCIA {0} (cont.): Resilience4j - recuperacion al volver core" -f $script:numero) -ForegroundColor Cyan
        Write-Host "  Se inicia core-banking-api otra vez: tras el tiempo en OPEN el circuito pasa a HALF_OPEN y, con llamadas exitosas, a CLOSED." -ForegroundColor White
        Write-Host ""
        Show-Comando 'docker compose start core-banking-api'
        docker compose start core-banking-api
        Write-Host ""
        for ($s = 20; $s -gt 0; $s--) {
            Write-Host -NoNewline ("`r    Esperando que core arranque: {0,2} s " -f $s) -ForegroundColor DarkGray
            Start-Sleep -Seconds 1
        }
        Write-Host ""
        # Core tarda ~25-30 s en arrancar: si aun no responde, se espera un poco mas (401 sin token = ya esta arriba)
        $extra = 0
        while ($extra -lt 60 -and "$(& curl.exe -s -o NUL -w '%{http_code}' -m 3 "http://localhost:8443/internal/cuentas/$cuenta/saldo")" -ne "401") {
            Write-Host -NoNewline ("`r    core aun arrancando, espera adicional: {0,2} s " -f $extra) -ForegroundColor DarkGray
            Start-Sleep -Seconds 2; $extra += 2
        }
        if ($extra -gt 0) { Write-Host "" }
        Write-Host ""
        Show-Comando "curl.exe -s -o NUL -w `"%{http_code}`" http://localhost:8444/cuentas/$cuenta/saldo -H `"Authorization: Bearer `$tokenWeb`"   (x4)"
        for ($i = 1; $i -le 4; $i++) {
            Show-Resultado ("Consulta {0}" -f $i) (Get-CodigoSaldoWeb $tokenWeb) "200"
        }
        Wait-Captura

        if (-not $SinPausas) { Clear-Host }
        Write-Host ""
        Write-Host ("  EVIDENCIA {0} (cont.): Resilience4j - transiciones del circuit breaker" -f $script:numero) -ForegroundColor Cyan
        Write-Host "  Log de bbf-web: CLOSED -> OPEN (core caido), OPEN -> HALF_OPEN (15 s despues), HALF_OPEN -> CLOSED (core de vuelta)." -ForegroundColor White
        Write-Host ""
        Show-Comando 'docker compose logs bbf-web | findstr /i "CircuitBreaker"   (ultimas 10)'
        docker compose logs bbf-web --no-log-prefix | findstr /i "CircuitBreaker" | Select-Object -Last 10
        Wait-Captura
    }
    finally {
        # Nunca dejar core detenido, aunque el script se interrumpa en esta seccion
        if (-not (Test-ServicioArriba "core-banking-api")) {
            Write-Host "Reiniciando core-banking-api (quedo detenido)..." -ForegroundColor Yellow
            docker compose start core-banking-api
        }
    }

    # ===================== 7. Batch =====================
    Start-Seccion "Batch - bankbatchprocessor" `
        "El servicio one-shot bankbatchprocessor ejecuta sus 3 jobs de Spring Batch (CSV -> MySQL) y termina con Exited (0)."
    Show-Comando 'docker compose ps -a bankbatchprocessor'
    docker compose ps -a bankbatchprocessor --format "table {{.Service}}`t{{.Status}}"
    Write-Host ""
    Show-Comando 'docker compose logs bankbatchprocessor | findstr /i "COMPLETED"'
    docker compose logs bankbatchprocessor --no-log-prefix | findstr /i "COMPLETED"
    Wait-Captura "Fin de las pruebas. Presiona Enter para salir"
}
finally {
    if (Test-Path $tmp) { Remove-Item $tmp -Force }
    Pop-Location
}
