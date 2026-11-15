#!/usr/bin/env bash
# Prepara e inicia o Vagora. Pode rodar quantas vezes quiser: só refaz o que mudou.
#
#   ./run.sh               inicia e abre o navegador
#   ./run.sh --no-browser  inicia sem abrir o navegador
#   ./run.sh --clean       apaga o build e compila tudo do zero antes de iniciar
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
PORT=8080

ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m✗\033[0m %s\n' "$1"; [ $# -gt 1 ] && printf '    %s\n' "${@:2}"; exit 1; }

echo "Verificando o que o Vagora precisa..."

# --- Java 21+ ---
command -v java >/dev/null || fail "Java não encontrado." "Instale o Java 21 ou mais novo: https://adoptium.net"
java_version=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
[ "${java_version:-0}" -ge 21 ] || fail "Java $java_version encontrado, mas é preciso o 21 ou mais novo." "Baixe em https://adoptium.net"
ok "Java $java_version"

# --- Python 3.9+ ---
command -v python3 >/dev/null || fail "Python 3 não encontrado." "Instale o Python 3.9 ou mais novo: https://www.python.org/downloads"
python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 9) else 1)' \
    || fail "É preciso o Python 3.9 ou mais novo (encontrado: $(python3 --version 2>&1))."
ok "$(python3 --version)"

# --- Google Chrome (o scraper abre as vagas nele) ---
chrome=""
for candidate in google-chrome google-chrome-stable; do
    command -v "$candidate" >/dev/null && chrome=$candidate && break
done
[ -z "$chrome" ] && [ -d "/Applications/Google Chrome.app" ] && chrome="Google Chrome.app"
[ -n "$chrome" ] || fail "Google Chrome não encontrado." "O Vagora usa o Chrome para abrir as vagas do LinkedIn: https://www.google.com/chrome"
ok "Google Chrome"

# --- Ambiente Python do scraper: criado na primeira vez, refeito se as dependências mudarem ---
requirements_hash=$(python3 -c 'import hashlib,sys; print(hashlib.sha256(open(sys.argv[1],"rb").read()).hexdigest())' "$REQUIREMENTS")
installed_hash=$(cat "$VENV/.requirements.sha256" 2>/dev/null || true)

if [ ! -x "$VENV/bin/python" ]; then
    echo "  Criando o ambiente Python (só na primeira vez)..."
    python3 -m venv "$VENV" 2>/dev/null \
        || fail "Não foi possível criar o ambiente Python." "No Ubuntu/Debian, instale com: sudo apt install python3-venv"
    installed_hash=""
fi

if [ "$requirements_hash" != "$installed_hash" ]; then
    echo "  Instalando as dependências do scraper..."
    "$VENV/bin/python" -m pip install --quiet --upgrade pip
    "$VENV/bin/python" -m pip install --quiet -r "$REQUIREMENTS"
    echo "$requirements_hash" > "$VENV/.requirements.sha256"
fi
ok "Ambiente Python do scraper"

# --- Porta livre ---
if (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then
    fail "A porta $PORT já está em uso." "O Vagora já está aberto? Acesse http://localhost:$PORT ou feche o outro programa."
fi

# --- Caches ---
# Python: bytecode compilado, barato de recriar. A sessão do LinkedIn (python/.profile) é mantida
# de propósito: apagá-la faz o LinkedIn pedir login e verificação de novo.
find python -name __pycache__ -type d -prune -not -path "$VENV/*" -exec rm -rf {} +
if [ "$CLEAN" = 1 ]; then
    echo "  Apagando o build anterior..."
    ./mvnw -q clean
fi
ok "Caches limpos"

# --- Build: só quando o código mudou desde o último ---
if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
    echo "  Compilando o Vagora (a primeira vez demora alguns minutos)..."
    ./mvnw -q package -DskipTests || fail "A compilação falhou. Veja as mensagens acima."
fi
ok "Vagora compilado"

# Abre o navegador padrão quando a aplicação responder. Use --no-browser para não abrir.
open_browser() {
    local url="http://localhost:$PORT"
    for _ in $(seq 1 60); do
        (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null && break
        sleep 1
    done
    case "$(uname -s)" in
        Darwin) open "$url" ;;
        Linux)  [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] && xdg-open "$url" ;;
        MINGW*|MSYS*|CYGWIN*) cmd.exe /c start "$url" ;;
    esac
} >/dev/null 2>&1

echo
echo "O Vagora abre no navegador em instantes: http://localhost:$PORT"
echo "Para parar, use Ctrl+C."
echo
[ "$OPEN_BROWSER" = 1 ] && open_browser &
exec java -jar "$JAR"
