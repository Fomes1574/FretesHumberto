# Plano de implementação Android

**Em andamento desde 30/10/2026.** Etapa 1 parcialmente pronta (ver abaixo); etapas 2 a 6 por fazer. Os requisitos do produto já estão definidos em [REGRAS_DE_NEGOCIO.md](REGRAS_DE_NEGOCIO.md). Esta sequência organiza a construção sem ampliar o escopo ou pressupor APK existente.

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
- [x] 39 testes JUnit em `core/src/test` com os mesmos casos do protótipo, **todos passando** (compilados com Kotlin 2.0.21 fora do Gradle; rodar de novo com `./gradlew :core:test`).
- [x] Esqueleto do app: quatro abas com ícone e texto, tema bege/verde, Atkinson Hyperlegible em `res/font`, ícone adaptativo com a Fiorino, backup do Google configurado.
- [x] Workflow do GitHub Actions: testes + APK debug a cada push; APK release assinado quando houver Secrets.

**Falta para fechar a etapa 1:**

- [ ] Primeiro `./gradlew :core:test :app:assembleDebug` verde (Actions ou Android Studio). O `:app` ainda não foi compilado.
- [ ] Abrir o APK debug num emulador/celular e ver as quatro abas.
- [ ] Ajustar qualquer versão que o sync recusar e registrar em DECISOES.md.

**Saída:** testes de domínio passam no Gradle; APK debug abre; nenhum servidor ou IA é necessário.

### 2. Banco e operações

- Criar entidades, DAOs, índices e esquema exportado do Room.
- Implementar salvar/editar/excluir/desfazer e revisão dos dias, com transações.
- Cobrir mover frete entre datas sem mexer nos gastos e falha de gravação sem falso sucesso.
- Preservar IDs e definir migrações. Não usar fallback destrutivo para resolver divergência.

**Saída:** operações e migrações testadas; totais derivados de uma única base.

### 3. Telas e acessibilidade

- Reproduzir Hoje, Histórico, Relatórios e Ajustes conforme o protótipo vigente.
- Formulários simples; somente detalhes adicionais sob expansão. Teclado decimal, data clara, foco e retorno corretos.
- Relatórios equivalentes, seleção de dois dias, valores textuais e gráficos com metas históricas.
- Cena Fiorino em Canvas, com redução de movimento e sem redesenho contínuo em repouso.
- Testar letra ampliada, Android font scale, TalkBack, teclado aberto e telas pequenas.

**Saída:** conferir registros, valores, ações e navegação sem corte no aparelho.

### 4. Contas do mês

- Aplicar versões e exceções em transação, com índice único conta+mês.
- Worker e abertura usam a mesma geração idempotente, sem exigir horário exato.
- Processar atrasos após dias sem abrir; respeitar mês pulado, pausa e data final.
- Testar concorrência entre Worker/abertura, dia 31 e fevereiro bissexto com regra de dia 28.

**Saída:** execução repetida/concorrente não duplica; histórico anterior permanece.

### 5. Cópia e restauração

- Definir DTO versionado e validação completa. **Não é preciso importar cópias do protótipo** (decisão de 30/10/2026: o motorista não usa o protótipo com dados reais).
- Usar seletor de arquivo Android; confirmar stream gravado antes de atualizar data da cópia.
- Restaurar com validação e substituição transacional; se falhar, preservar banco anterior.
- Testar senha incorreta, adulteração, formato futuro, arquivo truncado, cancelamento, pouco espaço e restauração de todas as versões/exceções.
- Configurar política explícita de backup/transferência do sistema Android; banco local não garante que o sistema não faça cópia em nuvem.

**Saída:** salvar, localizar e restaurar arquivo no aparelho, com e sem senha; não basta teste criptográfico de memória.

### 6. Instalação e piloto

- Produzir APK assinado com chave estável fora do repositório.
- Testar instalação e atualização por cima com preservação dos dados e restauração em instalação limpa.
- Testar uso offline, retorno após fechar processo, virada de dia/semana/mês e desempenho com histórico maior.
- Fazer piloto com o motorista. Ajustar dificuldades observadas sem inventar métricas ou tempo de uso.
- Documentar aparelho, versão, commit, testes reais e pendências antes de marcar versão pronta.

**Saída:** aceite do uso no aparelho; só então tratar como primeira versão Android.

## Distribuição e custo

Protótipo atual funciona sem serviço de hospedagem. O APK poderá ser construído localmente ou por CI gratuito quando disponível; verificar conta/limites antes de configurar GitHub Actions. Não prometer execução gratuita ilimitada nem contratar alternativa paga automaticamente. Não é necessário publicar na Play Store para instalação familiar por APK.

Chave de assinatura estável, duas cópias seguras e nenhum segredo no Git. Dados reais não entram em fixtures, prints públicos ou relatórios do repositório.

## Fora da primeira implementação

CSV/PDF e manutenção por km são melhorias posteriores. Biometria é opcional futura; sincronização adicionaria conta e internet e não faz parte do projeto atual. Não incluir integração Lalamove, receivables ou módulo de investimentos por inferência.
