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

## O que continua sendo escolha técnica de implementação

Não falta nova decisão sobre fretes e despesas. Ainda será necessário registrar: versões compatíveis das ferramentas Android, SDK mínimo/target, applicationId, esquema/migrações finais, política de backup do sistema, formato/migração de cópia Android e estratégia de build disponível na conta. Essas escolhas não justificam reabrir regras já decididas.

Melhorias futuras, como sincronização, devem aparecer como proposta separada com impacto em custo e simplicidade; não acrescentar ao escopo atual automaticamente.
