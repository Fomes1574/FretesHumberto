# Chave de assinatura do APK

O Android só instala uma atualização **por cima** da versão anterior (sem apagar os dados) se as duas estiverem assinadas com a **mesma chave**. Perder a chave = não conseguir mais atualizar o app no celular do motorista sem desinstalar, ou seja, sem perder o banco.

Faça isto **uma vez**, antes de instalar a primeira versão de verdade no celular dele.

## 1. Criar a chave (no seu computador)

Precisa do JDK 17 (vem com o Android Studio). No terminal:

```sh
keytool -genkeypair -v \
  -keystore freteshumberto-release.jks \
  -alias meusfretes \
  -keyalg RSA -keysize 4096 \
  -validity 36500
```

- Escolha uma senha forte e anote num gerenciador de senhas.
- `-validity 36500` = 100 anos.
- **Nunca** coloque o `.jks` dentro da pasta do repositório. O `.gitignore` já bloqueia `*.jks`, mas guarde fora mesmo assim.

## 2. Guardar duas cópias

1. Num gerenciador de senhas ou pendrive guardado em casa.
2. Num lugar na nuvem que só você acessa (Google Drive pessoal, por exemplo).

Junto da chave, guarde: senha do arquivo, alias (`meusfretes`) e senha da chave.

## 3. Configurar o GitHub Actions

No repositório: **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Valor |
|---|---|
| `FRETES_KEYSTORE_BASE64` | Conteúdo do `.jks` em base64 (comando abaixo) |
| `FRETES_KEYSTORE_PASSWORD` | Senha do arquivo |
| `FRETES_KEY_ALIAS` | `meusfretes` |
| `FRETES_KEY_PASSWORD` | Senha da chave |

Gerar o base64:

```sh
# Linux
base64 -w0 freteshumberto-release.jks > chave.txt
# macOS
base64 -i freteshumberto-release.jks -o chave.txt
# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("freteshumberto-release.jks")) > chave.txt
```

Cole o conteúdo de `chave.txt` no Secret e **apague o `chave.txt`** depois.

## 4. Gerar uma versão

1. Aumente `versionCode` (sempre +1) e `versionName` em `app/build.gradle.kts`.
2. Anote a mudança no `CHANGELOG.md`.
3. Crie a tag e envie:

```sh
git tag v0.1.0
git push origin v0.1.0
```

O workflow `Android` gera `MeusFretes-v0.1.0.apk` assinado e publica na página **Releases** do repositório.

## 5. Build release local (opcional)

Crie `keystore.properties` na raiz do repositório (já ignorado pelo Git):

```properties
storeFile=../caminho/fora/do/repo/freteshumberto-release.jks
storePassword=...
keyAlias=meusfretes
keyPassword=...
```

E rode `./gradlew :app:assembleRelease`.

## Sobre o APK debug

O APK debug gerado a cada push usa uma chave temporária do próprio GitHub, diferente a cada execução, e tem o ID `io.github.fomes1574.freteshumberto.debug`. Ele serve **só para testar**: instala ao lado da versão real e não atualiza por cima de outro debug gerado em outra execução. No celular do motorista, instale apenas o APK de **Releases**.
