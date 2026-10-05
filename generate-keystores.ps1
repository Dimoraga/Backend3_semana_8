<#
.SYNOPSIS
    Genera todo el material TLS/JWT que necesitan los 4 servicios del BFF
    (core-banking-api, bbf-web, bbf-mobile, bbf-cajeros) para correr sobre
    HTTPS con certificados autofirmados y JWT firmados con RS256.

.DESCRIPTION
    1. Un keystore TLS PKCS12 autofirmado por servicio (puerto propio).
    2. El certificado TLS de core-banking-api importado como truststore en
       cada uno de los 3 BFF, para que su RestClient confie en el al
       llamarlo por HTTPS.
    3. Un keypair RS256 dedicado (separado del TLS) para que core-banking-api
       firme los JWT.
    4. El certificado que contiene esa llave publica RS256, copiado a los
       3 BFF para que validen la firma de los JWT localmente.

    Todos los .p12 quedan gitignored (ver .gitignore de cada proyecto); los
    certificados .cer/.pem se pueden commitear porque no son secretos.

.PARAMETER Force
    Regenera todo aunque ya exista (por defecto el script no toca lo que
    ya esta generado).

.PARAMETER StorePassword
    Contraseña para los 4 keystores TLS y los truststores. Debe copiarse a
    mano en application-local.properties de cada servicio.
    Sin valor por defecto: se toma del parametro, de la variable de entorno
    KEYSTORE_STORE_PASSWORD o se pide por consola (sin eco). Minimo 6 caracteres.

.PARAMETER SigningKeyPassword
    Contraseña del keystore de firma JWT de core-banking-api (va tambien en
    JWT_KEYSTORE_PASSWORD del .env para Docker).
    Sin valor por defecto: se toma del parametro, de la variable de entorno
    KEYSTORE_SIGNING_PASSWORD o se pide por consola (sin eco). Minimo 6 caracteres.

.EXAMPLE
    .\generate-keystores.ps1
    (pide ambas contraseñas por consola)

.EXAMPLE
    $env:KEYSTORE_STORE_PASSWORD = "<clave TLS>"; $env:KEYSTORE_SIGNING_PASSWORD = "<clave JWT>"
    .\generate-keystores.ps1 -Force
#>
param(
    [switch]$Force,
    [string]$StorePassword,
    [string]$SigningKeyPassword
)

$ErrorActionPreference = "Stop"

# Resuelve una contrasena sin valor por defecto: parametro > variable de entorno > Read-Host -AsSecureString.
# Si no se obtiene ninguna (por ejemplo en modo no interactivo) falla con un mensaje claro.
function Resolve-Password {
    param([string]$Valor, [string]$Parametro, [string]$VariableEntorno, [string]$Descripcion)

    if (-not $Valor) { $Valor = [Environment]::GetEnvironmentVariable($VariableEntorno) }
    if (-not $Valor) {
        try {
            $seguro = Read-Host -AsSecureString "Contrasena para $Descripcion"
            $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
            try { $Valor = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
        } catch {
            $Valor = $null
        }
    }
    if (-not $Valor) {
        throw "Falta la contrasena para $Descripcion. Entregala con -$Parametro, con la variable de entorno $VariableEntorno o escribela cuando el script la pida."
    }
    if ($Valor.Length -lt 6) {
        throw "La contrasena para $Descripcion debe tener al menos 6 caracteres (requisito de keytool)."
    }
    return $Valor
}

$StorePassword = Resolve-Password -Valor $StorePassword -Parametro "StorePassword" `
    -VariableEntorno "KEYSTORE_STORE_PASSWORD" -Descripcion "los keystores TLS y truststores"
$SigningKeyPassword = Resolve-Password -Valor $SigningKeyPassword -Parametro "SigningKeyPassword" `
    -VariableEntorno "KEYSTORE_SIGNING_PASSWORD" -Descripcion "el keystore de firma JWT"

function Resolve-Keytool {
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) {
        return "$env:JAVA_HOME\bin\keytool.exe"
    }
    $roots = @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium")
    foreach ($root in $roots) {
        if (Test-Path $root) {
            $candidate = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName "bin\keytool.exe" } |
                Where-Object { Test-Path $_ } |
                Select-Object -First 1
            if ($candidate) { return $candidate }
        }
    }
    throw "No se encontro keytool.exe. Instala un JDK 21 o agrega su carpeta 'bin' al PATH (o define JAVA_HOME)."
}

$keytool = Resolve-Keytool
Write-Host "Usando keytool: $keytool`n"

$root = $PSScriptRoot
$dname = "CN=localhost, OU=DUOC, O=DUOC, L=Santiago, ST=RM, C=CL"

$services = @(
    @{ Name = "core-banking-api"; Alias = "core-tls" },
    @{ Name = "bbf-web"; Alias = "web-tls" },
    @{ Name = "bbf-mobile"; Alias = "mobile-tls" },
    @{ Name = "bbf-cajeros"; Alias = "cajero-tls" }
)
$bffs = @("bbf-web", "bbf-mobile", "bbf-cajeros")

function New-TlsKeystore {
    param([string]$ServiceName, [string]$Alias)

    $keysDir = Join-Path $root "$ServiceName\src\main\resources\keys"
    New-Item -ItemType Directory -Force -Path $keysDir | Out-Null
    $keystorePath = Join-Path $keysDir "$Alias.p12"

    if ((Test-Path $keystorePath) -and -not $Force) {
        Write-Host "  [skip] $keystorePath ya existe"
        return
    }
    if (Test-Path $keystorePath) { Remove-Item $keystorePath -Force }

    & $keytool -genkeypair -alias $Alias -keyalg RSA -keysize 2048 -validity 365 `
        -storetype PKCS12 -keystore $keystorePath -storepass $StorePassword `
        -dname $dname -ext "SAN=dns:localhost,ip:127.0.0.1" | Out-Null
    Write-Host "  [ok]   $keystorePath"
}

Write-Host "== 1. Keystores TLS (uno por servicio) =="
foreach ($s in $services) { New-TlsKeystore -ServiceName $s.Name -Alias $s.Alias }

Write-Host "`n== 2. Certificado TLS de core-banking-api -> truststore de cada BFF =="
$coreKeysDir = Join-Path $root "core-banking-api\src\main\resources\keys"
$coreKeystorePath = Join-Path $coreKeysDir "core-tls.p12"
$coreCertPath = Join-Path $coreKeysDir "core-tls.cer"

if ((-not (Test-Path $coreCertPath)) -or $Force) {
    if (Test-Path $coreCertPath) { Remove-Item $coreCertPath -Force }
    & $keytool -exportcert -alias core-tls -keystore $coreKeystorePath -storepass $StorePassword `
        -file $coreCertPath -rfc | Out-Null
    Write-Host "  [ok]   $coreCertPath"
}

foreach ($bff in $bffs) {
    $keysDir = Join-Path $root "$bff\src\main\resources\keys"
    New-Item -ItemType Directory -Force -Path $keysDir | Out-Null
    $truststorePath = Join-Path $keysDir "core-truststore.p12"

    if ((Test-Path $truststorePath) -and -not $Force) {
        Write-Host "  [skip] $truststorePath ya existe"
        continue
    }
    if (Test-Path $truststorePath) { Remove-Item $truststorePath -Force }

    & $keytool -importcert -alias core-tls -file $coreCertPath -keystore $truststorePath `
        -storepass $StorePassword -noprompt | Out-Null
    Write-Host "  [ok]   $truststorePath"
}

Write-Host "`n== 3. Keypair RS256 para firmar JWT (solo core-banking-api) =="
$signingKeystorePath = Join-Path $coreKeysDir "core-signing-key.p12"
if ((-not (Test-Path $signingKeystorePath)) -or $Force) {
    if (Test-Path $signingKeystorePath) { Remove-Item $signingKeystorePath -Force }
    & $keytool -genkeypair -alias core-signing-key -keyalg RSA -keysize 2048 -validity 365 `
        -storetype PKCS12 -keystore $signingKeystorePath -storepass $SigningKeyPassword `
        -dname "CN=core-signing-key, OU=DUOC, O=DUOC, L=Santiago, ST=RM, C=CL" | Out-Null
    Write-Host "  [ok]   $signingKeystorePath"
} else {
    Write-Host "  [skip] $signingKeystorePath ya existe"
}

Write-Host "`n== 4. Certificado con la llave publica RS256 -> copiado a los 3 BFF =="
$signingCertPath = Join-Path $coreKeysDir "core-signing-cert.pem"
if ((-not (Test-Path $signingCertPath)) -or $Force) {
    if (Test-Path $signingCertPath) { Remove-Item $signingCertPath -Force }
    & $keytool -exportcert -alias core-signing-key -keystore $signingKeystorePath `
        -storepass $SigningKeyPassword -file $signingCertPath -rfc | Out-Null
    Write-Host "  [ok]   $signingCertPath"
}
foreach ($bff in $bffs) {
    $destino = Join-Path $root "$bff\src\main\resources\keys\core-signing-cert.pem"
    Copy-Item -Path $signingCertPath -Destination $destino -Force
    Write-Host "  [ok]   $destino"
}

Write-Host "`n== Listo =="
Write-Host "Las contrasenas no se muestran: usa las mismas que entregaste a este script."
Write-Host ""
Write-Host "Falta completar a mano en cada application-local.properties:"
Write-Host "  - core-banking-api: server.ssl.key-store-password, corebanking.jwt.keystore-password, corebanking.jwt.key-password"
Write-Host "  - bbf-web / bbf-mobile / bbf-cajeros: server.ssl.key-store-password, corebanking.client.trust-store-password"
Write-Host "  - bbf-web / bbf-mobile / bbf-cajeros: spring.security.oauth2.client.registration.core.client-secret (igual al del cliente en auth-server)"
Write-Host "  - Y la contraseña de MySQL (spring.datasource.password) en core-banking-api"
