# Assinatura e atualização do Meus Fretes

Os APKs oficiais usam o identificador `io.github.fomes1574.freteshumberto`. O Android instala uma nova versão por cima quando ela tem o mesmo identificador e a mesma assinatura, com código de versão compatível. O projeto exige um `versionCode` maior em cada publicação. As migrações do banco Room mantêm os dados ao mudar o esquema.

## Configurar a chave uma vez no Windows

Use o Git, o [GitHub CLI](https://cli.github.com/) e o JDK 17 ou mais recente. O Android Studio normalmente já inclui o `keytool`.

Em uma cópia atualizada do repositório, rode no PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\configurar-assinatura.ps1
```

O script usa a conta administradora de `Fomes1574/FretesHumberto`. Se precisar, o GitHub CLI vai pedir que você autorize o acesso pelo navegador. Separe no gerenciador de senhas uma senha exclusiva, com pelo menos 16 caracteres ASCII e sem espaços. O PowerShell pede essa senha de forma oculta.

A chave é criada em `%LOCALAPPDATA%\MeusFretes\assinatura\freteshumberto-release.p12`. Guarde uma segunda cópia do arquivo e preserve a senha. Para executar o script em outro PC depois, coloque a cópia nesse caminho antes de rodá-lo. O script reaproveita a chave existente e interrompe se o certificado do arquivo for diferente do certificado cadastrado.

O GitHub CLI envia a chave e a senha aos Secrets do repositório. A variável `FRETES_SIGNING_CERT_SHA256` guarda apenas a impressão digital do certificado público:

| Configuração | Valor |
|---|---|
| Secret `FRETES_KEYSTORE_BASE64` | Arquivo PKCS#12 em base64 |
| Secret `FRETES_KEYSTORE_PASSWORD` | Senha do arquivo |
| Secret `FRETES_KEY_ALIAS` | `meusfretes` |
| Secret `FRETES_KEY_PASSWORD` | Senha da chave |
| Variável `FRETES_SIGNING_CERT_SHA256` | SHA-256 do certificado público |

## Publicar versões oficiais

1. Aumente `versionCode` e atualize `versionName` em `app/build.gradle.kts`. Registre a versão no [CHANGELOG](../CHANGELOG.md).
2. Depois de enviar o código para `main`, abra o workflow **Android** em **Actions**. Em **Run workflow**, selecione a `main` e marque **Publicar o APK oficial**. Uma tag `v` seguida da versão do app também dispara a publicação.
3. Os testes rodam antes da publicação. O workflow verifica a assinatura, compara com o APK anterior e exige um código de versão maior. Ele publica `MeusFretes.apk` no Release correspondente à versão. Cada versão oficial pode ser encontrada em [Releases](https://github.com/Fomes1574/FretesHumberto/releases). Depois da primeira publicação, o [link do APK mais recente](https://github.com/Fomes1574/FretesHumberto/releases/latest/download/MeusFretes.apk) serve para baixar as próximas versões.

Na configuração inicial, passar `-Publicar` ao script também dispara o workflow pela `main`.

## Instalação e conferência

- A edição de teste tem o identificador `io.github.fomes1574.freteshumberto.debug` e pode coexistir com a oficial. Quem tiver lançamentos de teste pode exportar um arquivo em **Ajustes → Fazer cópia** e importá-lo na edição oficial.
- Quem usa a edição oficial abre o novo `MeusFretes.apk` no Android e escolhe **Atualizar**.
- Para conferir a preservação dos dados, cadastre um frete, um abastecimento e um pedágio na primeira versão. Instale um APK oficial de código superior usando **Atualizar** e confira os três registros, o histórico, os totais e a edição.
- Os APKs de desenvolvimento estão no artefato `meus-fretes-debug` da aba **Actions**. O [Release de testes antigo](https://github.com/Fomes1574/FretesHumberto/releases/tag/meus-fretes) guarda as versões de depuração publicadas antes desse fluxo.
