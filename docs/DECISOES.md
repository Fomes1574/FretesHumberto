# Decisões do projeto

Consolidação em 30/10/2026. As regras atuais estão em [REGRAS_DE_NEGOCIO.md](REGRAS_DE_NEGOCIO.md); este registro explica por que tentativas antigas e imagens de referência podem divergir. Não é transcrição literal da conversa.

| Tema | Decisão vigente | Consequência para implementação |
|---|---|---|
| Público | Uso familiar por motorista com pouca experiência tecnológica | Poucos campos e ações, português comum e números grandes |
| Fase | Projeto e protótipo | Não gerar alegação de Android/APK pronto |
| Navegação | Hoje, Histórico, Relatórios, Ajustes | Contas mensais acessíveis sem quinta aba |
| Horário | Retirado dos relatórios | Data basta; duração opcional só como detalhe/simulação |
| Recebimento | Retirado “ainda não recebi” | Sem contas a receber nem status |
| Investimentos | Sem tratamento de CDB | Se registrado como despesa, segue saída comum |
| Relação de gastos | Fretes/despesas separados | Excluir/mover frete não propaga para gastos |
| Verificar uma viagem | Calculadora isolada | Sem salvar a simulação no diário |
| Rentabilidade | Somente resultados numéricos | Sem faixas “não cobre”, “abaixo”, “dentro do objetivo” |
| Combustível | Pelos abastecimentos no diário | Não descontar a estimativa novamente |
| Semana | Segunda a domingo completo | Próxima segunda fica fora |
| Consulta | Até seis meses por intervalo | Preservar dados antigos e permitir edição sem prazo |
| Dois dias | Soma e comparação só das duas datas | Não consultar automaticamente todo intervalo entre elas |
| Metas | Diária com vigência, mensal por mês | Não aplicar configurações novas retroativamente |
| Comparação | Recortes equivalentes quando parcial | Explicitar datas e parcialidade |
| Contas mensais | Na primeira versão | Ocorrência única, ajustes de dia e controle de exceções |
| Fevereiro | Dia 28 para vencimentos 29–31, inclusive bissexto | Preservar dia original para março; não “corrigir” para 29 |
| Cópias | Senha opcional e lembrete semanal interno | Exportação/restauração local, sem tarefa do ChatGPT |
| Visual | Preservar a direção atual e Fiorino | Não redesenhar o aplicativo sem solicitação |
| Custo | Zero para o projeto | Sem API/serviço pago obrigatório |
| Plataforma final | Kotlin/Compose/Room, offline | HTML é referência, não substituição silenciosa por WebView |

## Decisões técnicas fechadas em 30/10/2026

Tomadas pelo usuário para iniciar o Android. Não reabrir sem pedido dele.

| Tema | Decisão | Por quê / consequência |
|---|---|---|
| Valor na tela Hoje | **Valor final aparece na hora**; só a van anima | Fácil de conferir. Não reintroduzir contagem numérica |
| `minSdk` | **26** (Android 8.0) | Cobre praticamente todos os aparelhos em uso; habilita ícone adaptativo e `java.time` sem desugaring. Conferir a versão do celular do motorista antes do piloto |
| `targetSdk` / `compileSdk` | **36** (Android 16) | Requisito atual de API para apps; limite máximo do AGP 9.0. Subir para 37 junto com AGP 9.1.1+ numa etapa própria |
| `applicationId` | **`io.github.fomes1574.freteshumberto`** | Definitivo: trocar depois impede atualizar por cima. Debug usa sufixo `.debug` e pode ficar instalado junto |
| Backup automático do Google | **Ligado** (`allowBackup=true`, regras em `res/xml`) | Banco e preferências voltam ao trocar de celular, se o backup da conta Google estiver ativo. A cópia manual `.fretes` continua obrigatória no app |
| Dados do protótipo | **Não usar o protótipo com dados reais.** O Android começa do zero | A etapa 5 não precisa importar cópias do protótipo. Se algum dia for necessário, tratar como migração explícita e testada |
| Arquitetura de módulos | `:core` (Kotlin/JVM puro) + `:app` (Android) | Regras testáveis em segundos, sem emulador; o app só consome |
| Toolchain | AGP 9.0.1 (Kotlin embutido), Gradle 9.3.0, Kotlin 2.3.20, KSP 2.3.5, Compose BOM 2026.03.00, Room 2.8.4, DataStore 1.2.1, JDK 17 | Combinação compatível com compileSdk 36, conferida nas notas oficiais. Fonte: `gradle/libs.versions.toml` |
| Build do APK | GitHub Actions (repositório público) + Android Studio local | Workflow em `.github/workflows/android.yml`. APK de release assinado só com Secrets configurados |
| Assinatura | Uma chave para sempre, fora do Git, em duas cópias | Passo a passo em [CHAVE_ASSINATURA.md](CHAVE_ASSINATURA.md) |

## O que continua sendo escolha técnica de implementação

Esquema Room final e migrações, formato da cópia Android (DTO versionado) e detalhes de UI dentro da direção visual existente. Essas escolhas não justificam reabrir regras já decididas.

Melhorias futuras, como sincronização, devem aparecer como proposta separada com impacto em custo e simplicidade; não acrescentar ao escopo atual automaticamente. As propostas inspiradas em outros apps estão em [MELHORIAS_PESQUISADAS.md](MELHORIAS_PESQUISADAS.md) e só entram quando o usuário aprovar.
