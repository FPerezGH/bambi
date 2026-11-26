# CINEFRAN permanent signing. Run locally on Windows, outside any Git repository.
# Requires JDK 17 (keytool) and GitHub CLI authenticated as repository owner.
[CmdletBinding()]
param([string]$Directory = "$env:USERPROFILE\CINEFRAN-signing")
$ErrorActionPreference = "Stop"
$repo = "FPerezGH/bambi"
if (!(Get-Command keytool -ErrorAction SilentlyContinue)) { throw "Install JDK 17 and add keytool to PATH." }
if (!(Get-Command gh -ErrorAction SilentlyContinue)) { throw "Install GitHub CLI and run gh auth login." }
gh auth status
if ($LASTEXITCODE -ne 0) { throw "Authenticate with gh auth login." }
New-Item -ItemType Directory -Force -Path $Directory | Out-Null
$keystore = Join-Path $Directory "cinefran.p12"
$certificate = Join-Path $Directory "cinefran.cer"
if (Test-Path $keystore) { throw "Keystore already exists. Never overwrite or rotate it for an update." }
$existing = gh secret list -R $repo --json name | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Unable to inspect repository secrets." }
if ($existing.name -contains "CINEFRAN_KEYSTORE_BASE64") { throw "Signing secret already exists. Reuse the existing keystore instead of generating a new one." }
$secure = Read-Host "Choose a strong signing password (at least 16 letters/digits, retain securely)" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:CINEFRAN_LOCAL_PASS = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    if ($env:CINEFRAN_LOCAL_PASS -notmatch '^[A-Za-z0-9]{16,}$') { throw "Use at least 16 random alphanumeric characters." }
    keytool -genkeypair -keystore $keystore -storetype PKCS12 -alias cinefran -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=CINEFRAN" -storepass:env CINEFRAN_LOCAL_PASS -keypass:env CINEFRAN_LOCAL_PASS
    if ($LASTEXITCODE -ne 0) { throw "Key generation failed." }
    keytool -exportcert -keystore $keystore -alias cinefran -file $certificate -storepass:env CINEFRAN_LOCAL_PASS
    if ($LASTEXITCODE -ne 0) { throw "Certificate export failed." }
    $digest = (Get-FileHash $certificate -Algorithm SHA256).Hash.ToLowerInvariant()
    function Set-CinefranSecret([string]$Name,[string]$Value) {
        $Value | gh secret set $Name --repo $repo
        if ($LASTEXITCODE -ne 0) { throw "Could not set $Name. Retain the keystore and retry manually; do not generate another." }
    }
    Set-CinefranSecret "CINEFRAN_KEYSTORE_BASE64" ([Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore)))
    Set-CinefranSecret "CINEFRAN_STORE_PASSWORD" $env:CINEFRAN_LOCAL_PASS
    Set-CinefranSecret "CINEFRAN_KEY_PASSWORD" $env:CINEFRAN_LOCAL_PASS
    Set-CinefranSecret "CINEFRAN_KEY_ALIAS" "cinefran"
    gh variable set CINEFRAN_CERT_SHA256 --repo $repo --body $digest
    if ($LASTEXITCODE -ne 0) { throw "Could not set certificate fingerprint." }
    Write-Host "Signing configured. Keep $keystore and its password in an offline backup."
    Write-Host "Certificate SHA-256: $digest"
    Write-Host "To publish: gh workflow run build.yml --repo $repo --ref cinefran-stremio -f publish=true"
} finally {
    Remove-Item Env:CINEFRAN_LOCAL_PASS -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
