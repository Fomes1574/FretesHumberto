# Plano de implementação Android

**Em andamento desde 30/10/2026.** O código das etapas 1 a 5 está pronto e testado na JVM (GitHub Actions). Falta tudo o que depende de um celular: abrir o APK, conferir telas e cópia no aparelho, assinar e fazer o piloto (etapa 6). Os requisitos do produto já estão definidos em [REGRAS_DE_NEGOCIO.md](REGRAS_DE_NEGOCIO.md). Esta sequência organiza a construção sem ampliar o escopo ou pressupor APK existente.

## Direção

Kotlin, Jetpack Compose/Material 3, ViewModels, Room, DataStore e WorkManager. Um aplicativo local, com regras testáveis fora da UI e sem backend. Uma aplicação/módulo inicial é suficiente; não criar microserviços ou dividir em muitos módulos sem necessidade.

Versões escolhidas e registradas em `gradle/libs.versions.toml` (motivo em [DECISOES.md](DECISOES.md)). Se o primeiro sync falhar por uma versão, trocar pela estável compatível mais próxima e registrar a troca; não mudar a combinação inteira sem necessidade.

| Camada | Responsabilidade |
|---|---|
| UI/Compose | Formulários, quatro abas, estado e acessibilidade; sem cálculos duplicados em composables |
| ViewModels | Estado das telas e acionamento de operações; calculadora com estado isolado |
| Domínio | Dinheiro, datas, relatórios, metas e processamento de recorrência |
| Dados | Room/DAOs, transações, migrações e repositório único observado via Flow |
| Preferências | DataStore para opções sem histórico financeiro; metas históricas e ocorrências ficam no banco |
| Cópias | Serializar contrato versionado e gravar/ler arquivo pelo seletor do sistema |
| Agendamento | Worker e abertura chamam o mesmo caso de uso de geração de despesas devidas |

## Modelo de dados: detalhamento da proposta

Os nomes finais podem variar, preservando invariantes e migrações. Não existe esse schema pronto no repositório.

| Entidade | Campos/invariantes principais |
|---|---|
| Lançamento | ID estável, tipo, data local, valor em centavos `Long`, descrição, timestamps; valor positivo |
| Detalhe de frete | Chave do lançamento; km e minutos opcionais; nenhum vínculo com despesa |
| Detalhe de abastecimento | Chave do lançamento; litros em centésimos, preço em centavos, modo de entrada |
| Detalhe de despesa | Chave do lançamento e categoria; origem recorrente opcional, nunca freteId |
| Meta diária | Data de início única e valor; consultar maior vigência ≤ data |
| Meta mensal | Mês de referência único e valor; definir base inicial explicitamente |
| Recorrência | ID e início; versões mantêm nome, valor, dia original, atividade e fim |
| Versão da recorrência | Recorrência+mês de vigência únicos; preservar versões anteriores |
| Ocorrência mensal | Recorrência+mês únicos, status, lançamento opcional e exceção individual; mês pulado persistente |
| Revisão/conferência de dia | Data, revisão atual, revisão conferida e timestamp; alteração invalida marca |
| Referência manual do combustível | Preço, data financeira e timestamp para desempate; não reescreve abastecimentos |

Detalhes técnicos podem ter cascade ao seu próprio lançamento. **Nunca criar cascade de frete para despesa/abastecimento.** Ocorrência mensal deve continuar representando um mês pulado mesmo depois de excluir a despesa associada.

Guardar dinheiro em `Long` com verificação de limites/overflow; usar centésimos inteiros ou decimal exato nas medidas e resultados intermediários. Duração em minuto inteiro. Não converter tudo para `Double` e arredondar depois. Portar os casos de truncamento de entrada e arredondamento de divisão separadamente.

Data financeira é `LocalDate`/conversor equivalente, não timestamp UTC à meia-noite. Injetar relógio para testar virada de dia e fuso sem depender do relógio real. Timestamps de auditoria são instantes separados.

## Etapas e critérios de saída

### 1. Base e domínio

**Já feito (30/10/2026):**

- [x] Projeto Gradle com `:core` (Kotlin/JVM) e `:app` (Android/Compose), catálogo de versões e wrapper do Gradle 9.3.0.
- [x] SDK mínimo 26, target/compile 36, `applicationId` `io.github.fomes1574.freteshumberto` (DECISOES.md).
- [x] Regras portadas para Kotlin em `core/`: dinheiro, datas, resumo do período, metas, calculadora, combustível, preço de referência, contas do mês, diário (salvar/apagar/desfazer/conferir) e regras da estrada.
- [x] 39 testes JUnit em `core/src/test` com os mesmos casos do protótipo, **todos passando** no Gradle (`./gradlew :core:test`).
- [x] Esqueleto do app: quatro abas com ícone e texto, tema bege/verde, Atkinson Hyperlegible em `res/font`, ícone adaptativo com a Fiorino, backup do Google configurado.
- [x] Workflow do GitHub Actions: testes + APK debug a cada push; APK release assinado quando houver Secrets.

**Falta para fechar a etapa 1:**

- [x] Primeiro `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug` verde no GitHub Actions (30/10/2026, PR #1): testes do `:core` no Gradle e APK debug gerado.
- [ ] Abrir o APK debug num emulador/celular e ver as quatro abas.
- [x] Nenhuma versão precisou de ajuste: a combinação de `libs.versions.toml` compilou de primeira.

**Saída:** testes de domínio passam no Gradle; APK debug abre; nenhum servidor ou IA é necessário.

### 2. Banco e operações

- [x] Entidades, DAO, índices e esquema exportado do Room (versão 1 em `app/schemas/`, conferido pelo CI). Lançamento numa tabela só, com colunas opcionais por tipo e **nenhuma coluna que ligue gasto a frete**.
- [x] Salvar/editar/excluir/desfazer e revisão dos dias: `data/Repositorio.kt` lê o estado, aplica a regra do `:core` e grava só a diferença (`core/Estado.kt`, `Mudancas`), tudo numa transação.
- [x] Mover frete sem mexer nos gastos e falha de gravação sem falso sucesso: testes `RepositorioTest` (Robolectric) e `FluxosTest`; a tela só mostra "Salvo" depois da gravação.
- [x] IDs preservados; sem `fallbackToDestructiveMigration`. Lista `MIGRACOES` pronta para a versão 2.
- [ ] Primeira migração real (quando o esquema mudar): escrever `Migration(1, 2)` e teste com `MigrationTestHelper`.

**Saída:** operações testadas na JVM com Room em memória; totais sempre derivados do banco.

### 3. Telas e acessibilidade

- [x] Hoje, Histórico, Relatórios e Ajustes seguindo o protótipo vigente (`ui/`), mais Contas do mês, formulários e calculadora como telas por cima das abas.
- [x] Formulários simples, "Mais detalhes" para km e duração, teclado decimal, atalhos Hoje/Ontem e calendário sem datas futuras, proteção contra toque duplo.
- [x] Relatórios de dia, semana, mês, período (até 6 meses) e dois dias, com comparação equivalente, gráfico por dia com meta, valores em texto, combustível e 6 meses.
- [x] Cena da Fiorino em Canvas (`ui/estrada/CenaEstrada.kt`) com as regras de `core/Estrada.kt`, confete, borrão das rodas e sem animação com "Remover animações" ligado.
- [x] Letra A/A+/A++ por cima da fonte do Android; ícones sempre com texto; descrições para leitor de tela nos botões de ícone, gráficos e cena.
- [x] Fluxos principais testados na JVM (`FluxosTest`, Robolectric + Compose).
- [ ] Conferir no aparelho: letra A++ com fonte grande do Android, TalkBack, teclado aberto, tela pequena, cortes de texto.

**Saída:** conferir registros, valores, ações e navegação sem corte **no aparelho** (pendente).

### 4. Contas do mês

- [x] Versões e exceções aplicadas em transação (`core/Contas.kt` + repositório); chave conta+mês na tabela `ocorrencia` e índice único em `lancamento.ocorrencia`.
- [x] `contas/ContasWorker.kt` (WorkManager, a cada 12 h, sem horário exato) e a abertura/volta ao app chamam a mesma `Repositorio.gerarContas`.
- [x] Atrasos após dias sem abrir, mês pulado, pausa, retomar só um mês e data final: testes em `AppTest` e `RepositorioTest`.
- [x] Oito gerações simultâneas não duplicam (`RepositorioTest`); dia 31 e fevereiro bissexto no dia 28 (`RegrasTest`).
- [ ] Ver no aparelho a conta lançada com o app fechado (Worker rodando de verdade).

**Saída:** execução repetida/concorrente não duplica; histórico anterior permanece.

### 5. Cópia e restauração

- [x] Formato `.fretes` versionado (`core/Copia.kt`, versão 1) com validação completa. Não importa cópias do protótipo (decisão de 30/10/2026).
- [x] Seletor de arquivos do Android (`backup/ArquivosCopia.kt`); a gravação é conferida lendo o arquivo de volta antes de registrar "Última cópia".
- [x] Restauração validada antes de trocar os dados, numa transação; antes de trocar, o app guarda os dados atuais numa cópia interna para "Voltar aos dados de antes da restauração".
- [x] Testes de senha incorreta, adulteração, versão futura, arquivo cortado, IDs repetidos, ocorrência incoerente e restauração de versões/exceções (`AppTest`, `RepositorioTest`).
- [x] Política de backup do Android explícita no manifesto e em `res/xml` (backup do Google ligado, decisão de 30/10/2026).
- [ ] No aparelho: salvar no Drive/pasta/WhatsApp, localizar o arquivo e restaurar com e sem senha; cancelar o seletor; pouco espaço.

**Saída:** salvar, localizar e restaurar arquivo no aparelho, com e sem senha (pendente no aparelho).

### 6. Instalação e piloto

- [ ] Criar a chave de assinatura e os Secrets ([CHAVE_ASSINATURA.md](CHAVE_ASSINATURA.md)); gerar o APK assinado pela tag `v0.1.0`.
- [ ] Instalar, atualizar por cima mantendo os dados e restaurar em instalação limpa.
- [ ] Uso offline, fechar o processo e voltar, virada de dia/semana/mês e histórico grande.
- [ ] Piloto com o motorista. Ajustar dificuldades observadas sem inventar métricas ou tempo de uso.
- [ ] Documentar aparelho, versão, commit, testes reais e pendências antes de marcar versão pronta.

**Saída:** aceite do uso no aparelho; só então tratar como primeira versão Android.

## Distribuição e custo

Protótipo atual funciona sem serviço de hospedagem. O APK poderá ser construído localmente ou por CI gratuito quando disponível; verificar conta/limites antes de configurar GitHub Actions. Não prometer execução gratuita ilimitada nem contratar alternativa paga automaticamente. Não é necessário publicar na Play Store para instalação familiar por APK.

Chave de assinatura estável, duas cópias seguras e nenhum segredo no Git. Dados reais não entram em fixtures, prints públicos ou relatórios do repositório.

## Fora da primeira implementação

CSV/PDF e manutenção por km são melhorias posteriores. Biometria é opcional futura; sincronização adicionaria conta e internet e não faz parte do projeto atual. Não incluir integração Lalamove, receivables ou módulo de investimentos por inferência.
