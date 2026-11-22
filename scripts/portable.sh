#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

PYTHON=${PYTHON:-python3}
OUT=target/portable
VENV=target/leitor-venv

case "$(uname -s)" in
    Linux)  os=linux ;;
    Darwin) os=macos ;;
    *)      os=windows ;;
esac
case "$(uname -m)" in
    arm64|aarch64) arch=arm64 ;;
    *)             arch=x64 ;;
esac
zip_name="Vagora-$os-$arch.zip"

rm -rf "$OUT" target/leitor-dist target/leitor-build target/portable-input
mkdir -p "$OUT"

echo "Compilando o Vagora..."
./mvnw -q package -DskipTests

echo "Empacotando o leitor de vagas..."
[ -d "$VENV" ] || "$PYTHON" -m venv "$VENV"
py="$VENV/bin/python"
[ -x "$py" ] || py="$VENV/Scripts/python.exe"
"$py" -m pip install --quiet --upgrade pip
"$py" -m pip install --quiet -r python/requirements.txt pyinstaller==6.22.3
windowed=()
[ "$os" = windows ] && windowed=(--noconsole)
"$py" -m PyInstaller --noconfirm --onedir --name leitor --collect-all patchright ${windowed[@]+"${windowed[@]}"} \
    --distpath target/leitor-dist --workpath target/leitor-build --specpath target/leitor-build \
    python/hub.py

cp -r target/quarkus-app target/portable-input
[ "$os" = macos ] || cp -r target/leitor-dist/leitor target/portable-input/leitor

case "$os" in
    linux)   data='$APPDIR/../../dados'; scraper='$APPDIR/leitor/leitor'; dest="$OUT" ;;
    macos)   data='$APPDIR/../../../dados'; scraper='$APPDIR/../../../leitor/leitor'; dest="$OUT/Vagora" ;;
    windows) data='$APPDIR\..\dados'; scraper='$APPDIR\leitor\leitor.exe'; dest="$OUT" ;;
esac

echo "Gerando o app com o Java embutido..."
jpackage --type app-image --name Vagora --app-version 1.0.1 \
    --input target/portable-input --main-jar quarkus-run.jar --dest "$dest" \
    --java-options "-Dquarkus.profile=portable" \
    --java-options "-Dvagora.data=$data" \
    --java-options "-Dvagora.scraper.command=$scraper"

cp README.txt "$OUT/Vagora/"
[ "$os" = macos ] && cp -R target/leitor-dist/leitor "$OUT/Vagora/leitor"

echo "Compactando $zip_name..."
rm -f "target/$zip_name"
case "$os" in
    macos)   ditto -c -k --keepParent "$OUT/Vagora" "target/$zip_name" ;;
    windows) (cd "$OUT" && 7z a -tzip -bso0 "../$zip_name" Vagora) ;;
    *)       (cd "$OUT" && zip -qr "../$zip_name" Vagora) ;;
esac
echo "Pronto: target/$zip_name"
