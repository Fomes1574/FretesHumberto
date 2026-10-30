# Meus Fretes — guia para o Claude Code

@AGENTS.md

As regras acima (AGENTS.md) valem integralmente. Este arquivo acrescenta o caminho rápido para começar.

## Em uma frase

App Android offline para um motorista de Fiorino de 52 anos anotar fretes, abastecimentos e despesas e ver o lucro do dia, com metas, relatórios e a estrada animada da Fiorino. O protótipo web (`docs/prototipo.html`) é a referência de comportamento e visual.

## Onde estamos

- **Etapas 1 a 5 do [PLANO_ANDROID.md](docs/PLANO_ANDROID.md) com código pronto e testado na JVM** (30/10/2026): banco Room, telas, contas do mês com WorkManager, cópia de segurança. CI verde (66 testes no `:core`, 17 no `:app`) gera o APK debug.
- **Nada foi conferido num celular ainda.** As caixas `[ ]` que sobram são, quase todas, testes no aparelho e a etapa 6 (chave de assinatura, instalação, piloto).
- A próxima tarefa é sempre a primeira caixa `[ ]` do PLANO_ANDROID.md.
- Decisões técnicas fechadas: [docs/DECISOES.md](docs/DECISOES.md) → "Decisões técnicas fechadas em 30/10/2026". Não reabrir.

## Comandos

```sh
./gradlew :core:test                      # regras (rápido, sem Android)
./gradlew :app:testDebugUnitTest          # testes JVM do app
./gradlew :app:assembleDebug              # APK debug em app/build/outputs/apk/debug/
./gradlew :app:installDebug               # instala no emulador/celular conectado
npm test                                  # 32 testes do protótipo web (Node 22+)
```

Antes de dizer que algo está pronto: `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug` verde.

## Se estiver rodando na nuvem (Claude Code na web)

O build Android baixa dependências de fora. Se o Gradle falhar com erro de conexão/403, peça ao usuário para liberar estes domínios em **claude.ai → Configurações → Capabilities** (ou rode localmente com o Android Studio instalado):

`services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org`, `dl.google.com`, `maven.google.com`, `repo.maven.apache.org`, `repo1.maven.org`

Sem rede, ainda dá para trabalhar no `:core` (Kotlin puro) e no protótipo. Não invente que o build passou.

## Mapa rápido

| Preciso de… | Vá em |
|---|---|
| Regra de negócio (contrato) | `docs/REGRAS_DE_NEGOCIO.md` |
| Como a regra já está codificada | `core/src/main/kotlin/.../core/` (Dinheiro, Datas, Modelo, Calculos, Recorrencias, Diario, Contas, Estado, Copia, Textos, Estrada) |
| Banco e gravação | `app/.../data/` (`Repositorio` faz cada operação numa transação) |
| Telas | `app/.../ui/` (uma pasta por tela; `FretesApp` liga tudo; `FretesViewModel`) |
| Casos de teste de referência | `core/src/test/.../RegrasTest.kt`, `AppTest.kt` e `tests/regras.test.cjs` |
| Testes do app (Robolectric) | `app/src/test/.../data/RepositorioTest.kt` e `ui/FluxosTest.kt` |
| Visual (cores, tamanhos, telas) | `docs/ARQUITETURA.md` §3–5 + `docs/prototipo.html` + `docs/img/` |
| Tema Compose pronto | `app/.../ui/theme/Tema.kt` (cores, Atkinson Hyperlegible, tipografia) |
| Versões das bibliotecas | `gradle/libs.versions.toml` |
| Ideias de outros apps (não aprovadas) | `docs/MELHORIAS_PESQUISADAS.md` |
| Chave/APK release | `docs/CHAVE_ASSINATURA.md` |

## Convenções do código Android

- Pacote base `io.github.fomes1574.freteshumberto`. Regras em `.core`, nunca dentro de Composable ou ViewModel.
- Dinheiro em `Long` (centavos), litros e km em centésimos (`Long`), duração em minutos (`Int`), datas financeiras em `LocalDate`. Nada de `Double` para dinheiro.
- Cada operação do `Diario` (salvar, apagar, restaurar, conferir, gerar contas) = **uma transação Room**.
- Sem `fallbackToDestructiveMigration`. Esquema exportado em `app/schemas/` e versionado; o CI falha se o esquema gerado não estiver no Git (o log mostra o arquivo para copiar).
- Mudou uma tabela? Suba `version` em `BancoFretes`, acrescente a `Migration` em `MIGRACOES` e um teste.
- Textos da interface em português simples, curtos, sem julgamento ("compensa", "ruim"). Ícone sempre com texto.
- Tamanhos em `sp` (respeitam a fonte do Android). Áreas de toque ≥ 48 dp; ações principais 56–68 dp.
- Valor final do lucro aparece na hora; só a van anima. Regras da cena em `core/Estrada.kt`.
- Sem permissões novas no manifesto sem aprovação do usuário.

## Como trabalhar uma etapa

1. Leia a etapa no PLANO_ANDROID.md e as regras que ela toca.
2. Escreva/porte primeiro os testes; depois o código.
3. Rode os comandos acima. Se algo não puder ser verificado (ex.: sem emulador), diga isso.
4. Marque as caixas `[x]` concluídas no PLANO_ANDROID.md, atualize ESTADO_ATUAL.md e CHANGELOG.md.
5. Rode `python3 scripts/build_context.py` para regenerar o contexto completo.
6. Commits pequenos e em português, um assunto por commit.

Comandos prontos: `/proxima-etapa` e `/conferir` (em `.claude/commands/`).
