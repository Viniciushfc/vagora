$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$OpenBrowser = $true
$Clean = $false
foreach ($arg in $args) {
    switch ($arg) {
        "--no-browser" { $OpenBrowser = $false }
        "--clean" { $Clean = $true }
        { $_ -in "-h", "--help" } {
            "Prepara e inicia o Vagora. Pode rodar quantas vezes quiser: só refaz o que mudou."
            ""
            "  run.cmd               inicia e abre o Vagora em janela própria"
            "  run.cmd --no-browser  inicia sem abrir o navegador"
            "  run.cmd --clean       apaga o build e compila tudo do zero antes de iniciar"
            exit 0
        }
        default { Write-Host "Opção desconhecida: $arg (use --help)"; exit 1 }
    }
}

$Venv = "python\.venv"
$VenvPython = "$Venv\Scripts\python.exe"
$Requirements = "python\requirements.txt"
$Jar = "target\quarkus-app\quarkus-run.jar"
$Port = 7777

function Ok($msg) { Write-Host "  " -NoNewline; Write-Host "✓" -ForegroundColor Green -NoNewline; Write-Host " $msg" }
function Warn($msg, $hint) { Write-Host "  " -NoNewline; Write-Host "!" -ForegroundColor Yellow -NoNewline; Write-Host " $msg"; if ($hint) { Write-Host "    $hint" } }
function Fail($msg, $hint) { Write-Host "  " -NoNewline; Write-Host "✗" -ForegroundColor Red -NoNewline; Write-Host " $msg"; if ($hint) { Write-Host "    $hint" }; exit 1 }

function Refresh-Path {
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
}

function Install($what, $id) {
    $cmd = "winget install --exact --id $id --accept-source-agreements --accept-package-agreements"
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) { return }
    if ([Console]::IsInputRedirected) { Write-Host "    Para instalar: $cmd"; return }
    $answer = Read-Host "  $what não está instalado. Instalar agora com `"$cmd`"? [S/n]"
    if ($answer -and $answer -notmatch "^[SsYy]") { return }
    winget install --exact --id $id --accept-source-agreements --accept-package-agreements
    Refresh-Path
}

function Run-Quiet([scriptblock]$block) {
    $ErrorActionPreference = "Continue"
    try { & $block 2>&1 | Out-String } catch { "" }
}

Write-Host "Verificando o que o Vagora precisa..."

function Java-Version {
    $out = Run-Quiet { java -version }
    if ($out -match 'version "(\d+)') { [int]$Matches[1] } else { 0 }
}
function Java-Ok { (Get-Command javac -ErrorAction SilentlyContinue) -and (Java-Version) -ge 21 }
if (-not (Java-Ok)) { Install "Java 21 (JDK)" "EclipseAdoptium.Temurin.21.JDK" }
if (-not (Java-Ok)) { Fail "É preciso o Java 21 ou mais novo (JDK) no PATH." "Baixe em https://adoptium.net e abra um terminal novo." }
Ok "Java $(Java-Version)"

function Find-Python {
    foreach ($candidate in @(@("py", "-3"), @("python"))) {
        if (-not (Get-Command $candidate[0] -ErrorAction SilentlyContinue)) { continue }
        $exe = $candidate[0]
        $rest = @($candidate | Select-Object -Skip 1)
        $out = Run-Quiet { & $exe @rest -c "import sys, venv; print(sys.version_info >= (3, 9))" }
        if ($out -match "True") { return , $candidate }
    }
    $null
}
$Python = Find-Python
if (-not $Python) { Install "Python 3.9+" "Python.Python.3.12"; $Python = Find-Python }
if (-not $Python) { Fail "É preciso o Python 3.9 ou mais novo." "Instale em https://www.python.org/downloads e abra um terminal novo." }
$PyExe = $Python[0]
$PyArgs = @($Python | Select-Object -Skip 1)
Ok (Run-Quiet { & $PyExe @PyArgs --version }).Trim()

$requirementsHash = (Get-FileHash $Requirements -Algorithm SHA256).Hash.ToLower()
$installedHash = if (Test-Path "$Venv\.requirements.sha256") { (Get-Content "$Venv\.requirements.sha256" -Raw).Trim() } else { "" }

if (-not (Test-Path $VenvPython)) {
    Write-Host "  Criando o ambiente Python (só na primeira vez)..."
    & $PyExe @PyArgs -m venv $Venv
    if ($LASTEXITCODE -ne 0) { Fail "Não foi possível criar o ambiente Python." }
    $installedHash = ""
}

if ($requirementsHash -ne $installedHash) {
    Write-Host "  Instalando as dependências do scraper..."
    & $VenvPython -m pip install --quiet --upgrade pip
    & $VenvPython -m pip install --quiet -r $Requirements
    if ($LASTEXITCODE -ne 0) { Fail "Não foi possível instalar as dependências do scraper." }
    Set-Content "$Venv\.requirements.sha256" $requirementsHash
}
Ok "Ambiente Python do scraper"

function Find-Chrome {
    @("$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
      "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
      "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe") | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not (Find-Chrome)) { Install "Google Chrome" "Google.Chrome" }
$Chrome = Find-Chrome
if (-not $Chrome) { Fail "Google Chrome não encontrado." "O Vagora usa o Chrome para abrir as vagas do LinkedIn: https://www.google.com/chrome" }
Ok "Google Chrome"

if (Get-Command claude -ErrorAction SilentlyContinue) { Ok "Claude CLI" } else { Warn "Claude CLI não encontrado (opcional, só para usar o Claude)." "Instale em https://claude.com/claude-code" }

function Port-InUse($p) {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect("127.0.0.1", $p); $true } catch { $false } finally { $client.Dispose() }
}
while (Port-InUse $Port) {
    $page = try { (Invoke-WebRequest "http://localhost:$Port/" -UseBasicParsing -TimeoutSec 2).Content } catch { "" }
    if ($page -match "<title>Vagora</title>") { Fail "O Vagora já está aberto em http://localhost:$Port" "Use essa janela ou feche o outro antes de abrir de novo." }
    $Port++
}
Ok "Porta $Port"

Get-ChildItem python -Recurse -Directory -Filter __pycache__ | Where-Object { $_.FullName -notlike "*\.venv\*" } | Remove-Item -Recurse -Force
if ($Clean) {
    Write-Host "  Apagando o build anterior..."
    & .\mvnw.cmd -q clean
    if ($LASTEXITCODE -ne 0) { Fail "Não foi possível apagar o build anterior." }
}
Ok "Caches limpos"

$stale = -not (Test-Path $Jar)
if (-not $stale) {
    $built = (Get-Item $Jar).LastWriteTime
    $stale = [bool](@(Get-Item pom.xml) + @(Get-ChildItem src -Recurse -File) | Where-Object { $_.LastWriteTime -gt $built } | Select-Object -First 1)
}
if ($stale) {
    Write-Host "  Compilando o Vagora (a primeira vez demora alguns minutos)..."
    & .\mvnw.cmd -q package -DskipTests
    if ($LASTEXITCODE -ne 0) { Fail "A compilação falhou. Veja as mensagens acima." }
}
Ok "Vagora compilado"

$url = "http://localhost:$Port"
Write-Host ""
Write-Host "O Vagora abre no navegador em instantes: $url"
Write-Host "Para parar, use Ctrl+C."
Write-Host ""
if ($OpenBrowser) {
    $null = Start-Job -ArgumentList $Port, $url, $Chrome -ScriptBlock {
        param($port, $url, $chrome)
        foreach ($i in 1..60) {
            $client = New-Object Net.Sockets.TcpClient
            try { $client.Connect("127.0.0.1", $port); break } catch { Start-Sleep 1 } finally { $client.Dispose() }
        }
        Start-Process $chrome "--app=$url"
    }
}
& java "-Dquarkus.http.port=$Port" -jar $Jar
exit $LASTEXITCODE
