# O certificado público pode ser conferido no GitHub. A chave privada fica no Windows
# e é enviada diretamente aos Secrets, pelo GitHub CLI, sem aparecer no terminal.
[CmdletBinding()]
param(
    [switch]$Publicar
)

$ErrorActionPreference = 'Stop'
$repo = 'github.com/Fomes1574/FretesHumberto'
$aliasChave = 'meusfretes'
$nomesSecrets = @('FRETES_KEYSTORE_BASE64', 'FRETES_KEYSTORE_PASSWORD', 'FRETES_KEY_ALIAS', 'FRETES_KEY_PASSWORD')
$nomeCertificado = 'FRETES_SIGNING_CERT_SHA256'

$gh = Get-Command gh -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $gh) {
    throw 'Instale o GitHub CLI com winget install --id GitHub.cli -e e reabra o PowerShell.'
}
$keytool = Get-Command keytool -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if ($keytool) {
    $executavelJava = $keytool.Source
} else {
    $executavelJava = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr/bin/keytool.exe'
    if (-not (Test-Path -LiteralPath $executavelJava)) {
        throw 'Instale o JDK 17 ou o Android Studio para disponibilizar o keytool.'
    }
}

function Invoke-GitHub {
    & $gh.Source @args
    if ($LASTEXITCODE -ne 0) { throw "O GitHub CLI retornou erro em $($args[0]) $($args[1])." }
}

function Set-SecretDoGitHub([string]$Nome, [string]$Valor) {
    $Valor | & $gh.Source secret set $Nome --repo $repo
    if ($LASTEXITCODE -ne 0) { throw "Falha ao gravar o Secret $Nome." }
}

$autenticado = & {
    # O CLI escreve avisos em stderr mesmo em comandos consultivos no Windows PowerShell.
    $ErrorActionPreference = 'Continue'
    & $gh.Source auth status --hostname github.com *> $null
    $LASTEXITCODE -eq 0
}
if (-not $autenticado) {
    Invoke-GitHub auth login --hostname github.com --web --git-protocol https
}
$admin = Invoke-GitHub api --hostname github.com repos/Fomes1574/FretesHumberto --jq '.permissions.admin'
if ($admin -ne 'true') {
    throw 'Entre no GitHub CLI com uma conta administradora de Fomes1574/FretesHumberto.'
}

$variaveis = Invoke-GitHub variable list --repo $repo --json name,value | ConvertFrom-Json
$existente = $variaveis | Where-Object { $_.name -eq $nomeCertificado }
$secrets = Invoke-GitHub secret list --repo $repo --json name | ConvertFrom-Json
$assinaturaExistente = $secrets | Where-Object { $_.name -in $nomesSecrets }

$pasta = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'MeusFretes/assinatura'
$arquivo = Join-Path $pasta 'freteshumberto-release.p12'
$novaChave = -not (Test-Path -LiteralPath $arquivo -PathType Leaf)
if ($novaChave -and ($existente -or $assinaturaExistente)) {
    throw "Já há uma configuração de assinatura no GitHub. Recupere a chave original em $arquivo antes de continuar."
}

$senhaSegura = Read-Host 'Digite a senha da chave, guardada no seu gerenciador de senhas' -AsSecureString
$senha = [System.Net.NetworkCredential]::new('', $senhaSegura).Password
if ($senha -cnotmatch '^[!-~]{16,}$') {
    throw 'Use pelo menos 16 caracteres ASCII imprimíveis, sem espaços, para a senha da chave.'
}

$certificado = [IO.Path]::GetTempFileName()
try {
    $env:FRETES_LOCAL_SIGNING_PASSWORD = $senha
    if ($novaChave) {
        $confirmacao = Read-Host 'Repita a senha da chave' -AsSecureString
        if ($senha -cne [System.Net.NetworkCredential]::new('', $confirmacao).Password) {
            throw 'As duas senhas são diferentes.'
        }
        New-Item -Path $pasta -ItemType Directory -Force | Out-Null
        & $executavelJava -genkeypair -storetype PKCS12 -keystore $arquivo -alias $aliasChave `
            -keyalg RSA -keysize 4096 -validity 36500 -dname 'CN=Meus Fretes' `
            -storepass:env FRETES_LOCAL_SIGNING_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível gerar o arquivo da chave.' }
    }

    & $executavelJava -exportcert -storetype PKCS12 -keystore $arquivo -alias $aliasChave `
        -storepass:env FRETES_LOCAL_SIGNING_PASSWORD -file $certificado
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível abrir a chave. Confira a senha.' }
    $impressao = (Get-FileHash -LiteralPath $certificado -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($existente -and $existente.value -ne $impressao) {
        throw 'A chave do Windows é diferente da chave já registrada no GitHub. Recupere a cópia original.'
    }

    Set-SecretDoGitHub 'FRETES_KEYSTORE_BASE64' ([Convert]::ToBase64String([IO.File]::ReadAllBytes($arquivo)))
    Set-SecretDoGitHub 'FRETES_KEYSTORE_PASSWORD' $senha
    Set-SecretDoGitHub 'FRETES_KEY_ALIAS' $aliasChave
    Set-SecretDoGitHub 'FRETES_KEY_PASSWORD' $senha
    Invoke-GitHub variable set $nomeCertificado --repo $repo --body $impressao
} finally {
    Remove-Item Env:FRETES_LOCAL_SIGNING_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $certificado -ErrorAction SilentlyContinue
    $senha = $null
}

Write-Host "Chave pronta em $arquivo"
Write-Host "Certificado SHA-256: $impressao"
Write-Host 'Guarde uma segunda cópia do arquivo em local seguro junto com a senha no seu gerenciador.'

if ($Publicar) {
    Invoke-GitHub workflow run android.yml --repo $repo --ref main -f publicar_assinado=true
}
