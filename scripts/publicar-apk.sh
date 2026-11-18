#!/usr/bin/env bash
set -euo pipefail

falhar() {
  echo "::error::$1" >&2
  exit 1
}

apk=${1:?Informe o APK oficial}
certificado=${FRETES_SIGNING_CERT_SHA256:-}
certificado=${certificado,,}
[[ "$certificado" =~ ^[a-f0-9]{64}$ ]] || falhar "Configure a variável FRETES_SIGNING_CERT_SHA256."
ferramentas="$ANDROID_HOME/build-tools/36.0.0"

impressao() {
  "$ferramentas/apksigner" verify --print-certs "$1" |
    sed -n 's/^Signer #[0-9]* certificate SHA-256 digest: //p'
}

identidade() {
  "$ferramentas/aapt2" dump badging "$1" | sed -n '/^package: /p'
}

pacote="io.github.fomes1574.freteshumberto"
linha=$(identidade "$apk")
[[ "$linha" == "package: name='$pacote' "* ]] || falhar "O APK não tem o identificador da edição oficial."
codigo=$(sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p" <<< "$linha")
versao=$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<< "$linha")
[[ "$codigo" =~ ^[1-9][0-9]*$ && "$versao" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
  falhar "O APK tem uma versão inválida."
[[ "$(impressao "$apk")" == "$certificado" ]] ||
  falhar "O certificado do APK é diferente do certificado autorizado."

tag="v$versao"
opcoes=(--target "$GITHUB_SHA")
if [[ "$GITHUB_REF" == refs/tags/* ]]; then
  [[ "$GITHUB_REF_NAME" == "$tag" ]] || falhar "A tag deve corresponder à versão $tag do APK."
  opcoes=(--verify-tag)
fi

pasta=$(mktemp -d)
trap 'rm -rf -- "$pasta"' EXIT
anterior=$(gh release list --exclude-drafts --exclude-pre-releases --limit 1 --json tagName --jq '.[0].tagName // ""')
if [[ -n "$anterior" && "$anterior" != "meus-fretes" ]]; then
  gh release download "$anterior" --pattern MeusFretes.apk --dir "$pasta"
  publicado="$pasta/MeusFretes.apk"
  [[ "$(impressao "$publicado")" == "$certificado" ]] ||
    falhar "A assinatura não coincide com a última versão oficial."
  publicado_linha=$(identidade "$publicado")
  publicado_codigo=$(sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p" <<< "$publicado_linha")
  [[ "$publicado_linha" == "package: name='$pacote' "* && "$publicado_codigo" =~ ^[1-9][0-9]*$ ]] ||
    falhar "Não foi possível reconhecer a última versão oficial."
  (( codigo > publicado_codigo )) || falhar "Aumente versionCode antes de publicar."
fi

cp "$apk" "$pasta/MeusFretes.apk"
gh release create "$tag" "$pasta/MeusFretes.apk" "${opcoes[@]}" --latest \
  --title "Meus Fretes $versao" \
  --notes "Baixe **MeusFretes.apk**. Quem já usa a edição oficial pode instalar por cima para manter os lançamentos. Veja o CHANGELOG.md para as mudanças."
