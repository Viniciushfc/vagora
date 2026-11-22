#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

OPEN_BROWSER=1
CLEAN=0
for arg in "$@"; do
    case "$arg" in
        --no-browser) OPEN_BROWSER=0 ;;
        --clean) CLEAN=1 ;;
        -h|--help) sed -n '2,6p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Opção desconhecida: $arg (use --help)"; exit 1 ;;
    esac
done

VENV=python/.venv
REQUIREMENTS=python/requirements.txt
JAR=target/quarkus-app/quarkus-run.jar
PORT=7777

ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$1"; [ $# -gt 1 ] && printf '    %s\n' "${@:2}"; return 0; }
fail() { printf '  \033[31m✗\033[0m %s\n' "$1"; [ $# -gt 1 ] && printf '    %s\n' "${@:2}"; exit 1; }

PM=""
for candidate in apt-get dnf pacman zypper brew; do
    command -v "$candidate" >/dev/null && PM=$candidate && break
done

ask() {
    [ -t 0 ] || { printf '    Para instalar: %s\n' "$2"; return 1; }
    read -rp "  $1 Instalar agora com \"$2\"? [S/n] " answer
    [[ "${answer:-s}" =~ ^[SsYy] ]]
}

install() {
    local what=$1 pkgs="" cmd=""
    case "$PM" in
        apt-get) pkgs=$2; cmd="sudo apt-get install -y $pkgs" ;;
        dnf)     pkgs=$3; cmd="sudo dnf install -y $pkgs" ;;
        pacman)  pkgs=$4; cmd="sudo pacman -S --needed --noconfirm $pkgs" ;;
        zypper)  pkgs=$5; cmd="sudo zypper install -y $pkgs" ;;
        brew)    pkgs=$6; cmd="brew install $pkgs" ;;
    esac
    [ -n "$pkgs" ] && [ "$pkgs" != - ] || return 1
    ask "$what não está instalado." "$cmd" || return 1
    [ "$PM" = apt-get ] && sudo apt-get update -qq
    $cmd
}

echo "Verificando o que o Vagora precisa..."

java_version() { java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1; }
java_ok() { command -v javac >/dev/null && [ "$(java_version)" -ge 21 ] 2>/dev/null; }
brew_java() { [ "$PM" = brew ] && [ -d "$(brew --prefix)/opt/openjdk@21/bin" ] && PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"; }
java_ok || { brew_java || true; }
java_ok || { install "Java 21 (JDK)" openjdk-21-jdk java-21-openjdk-devel jdk21-openjdk java-21-openjdk-devel openjdk@21 && brew_java; } || true
java_ok || fail "É preciso o Java 21 ou mais novo (JDK) no PATH." "Baixe em https://adoptium.net"
ok "Java $(java_version)"

python_ok() { command -v python3 >/dev/null && python3 -c 'import sys, venv, ensurepip; sys.exit(sys.version_info < (3, 9))' 2>/dev/null; }
python_ok || install "Python 3.9+ com venv" "python3 python3-venv" python3 python python3 python || true
python_ok || fail "É preciso o Python 3.9 ou mais novo, com o módulo venv." "Instale em https://www.python.org/downloads"
ok "$(python3 --version)"

requirements_hash=$(python3 -c 'import hashlib,sys; print(hashlib.sha256(open(sys.argv[1],"rb").read()).hexdigest())' "$REQUIREMENTS")
installed_hash=$(cat "$VENV/.requirements.sha256" 2>/dev/null || true)

if [ ! -x "$VENV/bin/python" ]; then
    echo "  Criando o ambiente Python (só na primeira vez)..."
    python3 -m venv "$VENV" || fail "Não foi possível criar o ambiente Python."
    installed_hash=""
fi

if [ "$requirements_hash" != "$installed_hash" ]; then
    echo "  Instalando as dependências do scraper..."
    "$VENV/bin/python" -m pip install --quiet --upgrade pip
    "$VENV/bin/python" -m pip install --quiet -r "$REQUIREMENTS"
    echo "$requirements_hash" > "$VENV/.requirements.sha256"
fi
ok "Ambiente Python do scraper"

chrome_ok() {
    command -v google-chrome >/dev/null || command -v google-chrome-stable >/dev/null || [ -d "/Applications/Google Chrome.app" ]
}
if ! chrome_ok && ask "Google Chrome não está instalado." "$VENV/bin/python -m patchright install chrome"; then
    "$VENV/bin/python" -m patchright install chrome || true
fi
chrome_ok || fail "Google Chrome não encontrado." "O Vagora usa o Chrome para abrir as vagas do LinkedIn: https://www.google.com/chrome"
ok "Google Chrome"

if [ "$(uname -s)" = Linux ]; then
    command -v xvfb-run >/dev/null || install "Xvfb (deixa o navegador invisível)" xvfb xorg-x11-server-Xvfb xorg-server-xvfb xvfb-run - || true
    if command -v xvfb-run >/dev/null; then ok "Xvfb"; else warn "Sem Xvfb: a janela do Chrome aparece durante a análise."; fi
fi
if command -v claude >/dev/null; then ok "Claude CLI"; else warn "Claude CLI não encontrado (opcional, só para usar o Claude)." "Instale em https://claude.com/claude-code"; fi

while (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; do
    python3 -c 'import sys, urllib.request; sys.exit(b"<title>Vagora</title>" not in urllib.request.urlopen(sys.argv[1], timeout=2).read())' \
        "http://localhost:$PORT/" 2>/dev/null && fail "O Vagora já está aberto em http://localhost:$PORT" "Use essa aba ou feche o outro antes de abrir de novo."
    PORT=$((PORT + 1))
done
ok "Porta $PORT"

find python -name __pycache__ -type d -prune -not -path "$VENV/*" -exec rm -rf {} +
if [ "$CLEAN" = 1 ]; then
    echo "  Apagando o build anterior..."
    ./mvnw -q clean
fi
ok "Caches limpos"

if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
    echo "  Compilando o Vagora (a primeira vez demora alguns minutos)..."
    ./mvnw -q package -DskipTests || fail "A compilação falhou. Veja as mensagens acima."
fi
ok "Vagora compilado"

open_browser() {
    local url="http://localhost:$PORT"
    for _ in $(seq 1 60); do
        (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null && break
        sleep 1
    done
    case "$(uname -s)" in
        Darwin)
            for app in "Google Chrome" "Microsoft Edge"; do
                [ -d "/Applications/$app.app" ] && open -na "$app" --args --app="$url" && return
            done
            open "$url" ;;
        Linux)
            [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] || return
            for bin in google-chrome google-chrome-stable microsoft-edge chromium chromium-browser; do
                command -v "$bin" >/dev/null && setsid -f "$bin" --app="$url" && return
            done
            xdg-open "$url" ;;
        MINGW*|MSYS*|CYGWIN*)
            for exe in "${PROGRAMFILES:-}/Google/Chrome/Application/chrome.exe" "${LOCALAPPDATA:-}/Google/Chrome/Application/chrome.exe" \
                       "${PROGRAMFILES:-}/Microsoft/Edge/Application/msedge.exe" "${PROGRAMFILES:-} (x86)/Microsoft/Edge/Application/msedge.exe"; do
                [ -f "$exe" ] && { "$exe" --app="$url" & return; }
            done
            cmd.exe /c start "$url" ;;
    esac
} >/dev/null 2>&1

echo
echo "O Vagora abre no navegador em instantes: http://localhost:$PORT"
echo "Para parar, use Ctrl+C."
echo
[ "$OPEN_BROWSER" = 1 ] && open_browser &
exec java -Dquarkus.http.port="$PORT" -jar "$JAR"
