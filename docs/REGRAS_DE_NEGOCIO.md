# Regras de negócio — contrato do produto

Consolidação em 30/10/2026 das decisões acordadas. Estes requisitos guiam o protótipo e o futuro Android; não significam que o app nativo exista. Veja também [DECISOES.md](DECISOES.md).

## R01 — Lançamentos independentes

Três tipos: frete, abastecimento e despesa. Cada um tem ID, data financeira e valor próprios, cadastrado um a um. Pode anotar todos os fretes e depois os gastos, ou em outra ordem: a conta não muda.

Frete recebe o valor líquido que ficou para o motorista, sem descontar novamente a taxa já retida pela plataforma. Não haverá status de recebimento ou contas a receber.

Excluir um frete remove só ele e seus detalhes técnicos opcionais. Mudar sua data recalcula origem e destino. Nenhuma ação move, edita ou apaga despesas, abastecimentos ou outros fretes. O banco não terá chave de despesa para frete.

## R02 — Resultado financeiro

```text
lucro = soma dos fretes líquidos − soma dos abastecimentos − soma das outras despesas
```

Somar apenas as datas selecionadas. Despesas mensais entram como despesas comuns quando lançadas. Qualquer saída cadastrada como despesa reduz o resultado, independentemente do nome; sem módulo de investimentos.

Fretes R$ 285 − combustível R$ 40 − despesas R$ 20 = **R$ 225**. Com meta R$ 250, faltam **R$ 25**. Apagando só o frete, permanecem R$ 60 de gastos e o resultado fica **−R$ 60**. Nunca cortar prejuízo em zero.

“Lucro” é o resultado dos custos informados; não estima impostos, depreciação ou gastos ausentes. Um dia só com gastos aparece no histórico e reduz o período.

## R03 — Dinheiro, datas e edição

- Dinheiro em centavos inteiros; no Android não usar ponto flutuante para dinheiro.
- `R$ 1.234,56`, `1.234,56` e `1234,56` preservam R$ 1.234,56; `85.50` pode representar R$ 85,50. Rejeitar ambiguidade sem inventar valor.
- O parser atual considera `1.234` ambíguo: corrigir para `1234,00` ou `1.234,00`. Não transformar silenciosamente em R$ 1,23.
- Entrada limita a duas casas por truncamento: `54.0599` vira `54,05`. Resultados calculados de divisão são arredondados a duas casas; não confundir as operações.
- Valores de lançamentos são positivos; o tipo define entrada ou saída. Resultados podem ser negativos.
- Data financeira local sem horário: `AAAA-MM-DD`. Criação/edição técnica tem timestamp separado.
- **O dia vira às 00:00 pelo horário de Brasília** (`America/Sao_Paulo`), mesmo se o celular estiver em outro fuso. Depois da meia-noite, "hoje" é um dia novo, sem fretes nem despesas; os do dia anterior ficam no Histórico.
- Novos lançamentos começam em hoje, sem data financeira futura. Campos adicionais dependem do tipo: abastecimento exige preço ou litros; recorrência exige nome e repetição.
- Edição sem prazo de expiração. Qualquer lançamento antigo pode ser corrigido. Duração opcional em minutos inteiros: abrir/salvar 17 minutos mantém 17.

## R04 — Combustível

Abastecimento sai integralmente no dia em que foi cadastrado financeiramente, mesmo se o combustível for usado depois. Não ratear entre fretes ou dias.

```text
litros = valor pago ÷ preço por litro
preço por litro = valor pago ÷ litros
preço médio do período = soma dos valores pagos ÷ soma dos litros
percentual para o tanque = combustível ÷ fretes × 100
```

R$ 200 ÷ R$ 3,70 = **54,05 L**; R$ 200 ÷ 54,05 L = **R$ 3,70/L**, arredondados. Sem litros ou fretes, omitir indicador correspondente em vez de dividir por zero.

Guardar os dados de cada abastecimento. A referência de preço vem do mais recente por **data financeira**, com desempate técnico; lançamento antigo não pode tomar o lugar do mais novo. Editar/excluir o mais novo reavalia a referência. Ajustes aceita referência manual, sem alterar registros antigos. A precedência concreta atual está em `Core.price`.

## R05 — Calculadora isolada

Entradas: valor líquido, km totais incluindo coleta/retorno quando aplicável, consumo km/l, preço do álcool por litro, outros custos e duração opcional em minutos. Preço é informado ou reaproveitado localmente; não há consulta online.

```text
combustível estimado = km ÷ consumo × preço do litro
sobra = valor líquido − combustível estimado − outros custos
R$/km = sobra ÷ km
R$/hora = sobra × 60 ÷ minutos
```

Distância, consumo e preço positivos. Omitir R$/hora sem duração. R$ 80, 40 km, 8 km/l, R$ 4/l, 90 min → combustível **R$ 20**, sobra **R$ 60**, **R$ 1,50/km**, **R$ 40/h**.

Simular, limpar, fechar e alterar parâmetros não grava frete/despesa, não altera meta e não atualiza referência global. Sem botão “Fiz este frete, salvar”. Registrar viagem realizada em Anotar frete e gastos separadamente.

Sem “não cobre os custos”, “abaixo do objetivo”, “dentro do objetivo” ou semáforo de rentabilidade. Mostrar números. R$/km e R$/hora são exclusivos da simulação; relatórios não dividem gastos do período pelos km/minutos de fretes parcialmente preenchidos.

## R06 — Meta diária

Só existe **meta diária** (decisão de 30/10/2026: a meta mensal saiu de tudo). Mudar a meta vale **para o dia em que mudou e para os seguintes**; os dias que já passaram continuam com a meta que tinham. Ex.: às 23:30 ele fez vários fretes e sobe a meta de R$ 250 para R$ 300: aquele dia já passa a contar com R$ 300, os próximos também, e os dias anteriores continuam com R$ 250. A meta de dias passados não é editada.

Meta atingida quando lucro ≥ meta. Textos: “Faltam R$ X”, “Meta atingida”, “Meta atingida · R$ X acima”. R$ 250 é exemplo inicial, não valor fixo do produto.

Dia trabalhado = dia com pelo menos um frete. Só esses dias entram na contagem de metas. A média por dia trabalhado divide o resultado total do período por essa quantidade; o rótulo deve ser claro, pois o total inclui gastos em dias sem frete.

Corrigir lançamento antigo continua permitido e não muda a meta daquele dia. Não há marca de "dia conferido" (retirada em 30/10/2026).

## R07 — Períodos

| Seleção | Inclusão |
|---|---|
| Dia | Só a data escolhida |
| Semana | Segunda 00:00 até domingo completo, incluindo todo o minuto 23:59 |
| Mês | Primeiro ao último dia; atual identificado como parcial |
| Personalizado | Início/fim inclusos, no máximo seis meses de calendário |
| Dois dias | Só as duas datas distintas, sem os dias intermediários |

Semana tecnicamente `[segunda, segunda seguinte)`. No personalizado, data final deve ser **anterior a início + seis meses**, com ajuste de calendário. Aceitar 01/01–30/06, rejeitar 01/01–01/07. Não substituir por “180 dias”. Fim futuro não é aceito.

Pode consultar intervalos antigos; seis meses limitam largura da consulta, não idade dos dados nem edição.

Semana parcial compara os mesmos dias anteriores: segunda a quinta contra segunda a quinta. Mês parcial compara até o mesmo dia disponível no mês anterior. Períodos completos comparam completos. Identificar datas e parcialidade.

Relatórios com gráfico diário e valores em texto. Mês também mostra combustível nos seis meses até o mês escolhido. Sem filtro por horário; duração da viagem não cria relatórios por hora.

## R08 — Despesas mensais

Nome, valor, dia original, início e fim opcional. Cada mês devido produz despesa comum independente dos fretes. Futuro é previsão; não reduzir realizado antes da data.

| Dia original | Mês com 31 | Mês com 30 | Fevereiro, inclusive bissexto |
|---|---|---|---|
| 28 | 28 | 28 | 28 |
| 29 | 29 | 29 | 28 |
| 30 | 30 | 30 | 28 |
| 31 | 31 | 30 | 28 |

Guardar o dia original: janeiro 31 → fevereiro 28 → março 31. Não fixar permanentemente em 28 depois de fevereiro.

Unicidade por **recorrência+mês**. Abrir, retomar, restaurar e repetir agendamento não duplica. Processar meses vencidos ainda não gerados desde o início, respeitando vigências, fim, pausa e exceções. Não lançar vencimento anterior à data inicial.

| Ação | Efeito |
|---|---|
| Só este mês | Exceção naquela ocorrência, preservando as demais |
| Deste mês em diante | Nova versão da regra desde o mês selecionado; preservar anteriores e exceções individuais |
| Pular este mês | Não lançar, ou retirar só a ocorrência existente, guardando que foi pulada |
| Excluir despesa gerada | Pular aquela ocorrência para não ser recriada; manter regra e outros meses |
| Desfazer | Restaurar exatamente a ocorrência removida, sem duplicação |
| Pausar | Parar futuras gerações na vigência escolhida, preservando despesas já lançadas |
| Retomar só um mês | Exceção local, sem retomar automaticamente toda a repetição |

São registros locais, não pagamentos bancários. O protótipo processa ao abrir/retomar. No Android, WorkManager e abertura deverão compartilhar o mesmo processamento idempotente.

## R09 — Cópias e falhas

Dados no aparelho. Arquivo `.fretes` exportado manualmente, senha opcional e lembrete semanal dentro do app. Não criar automação externa do ChatGPT.

Cópia preserva lançamentos, IDs, detalhes, metas diárias históricas, versões de recorrência, exceções e meses pulados. Validar formato, versão, tipos, datas, limites, duplicidades e integridade antes de substituir. Falha mantém estado anterior.

Protótipo: AES-256-GCM, PBKDF2-SHA256 com 210 mil iterações, salt aleatório de 16 bytes e IV de 12 bytes. WebCrypto ou implementação incorporada compatível. Senha não é salva e não tem recuperação. Cópia sem senha é legível; a cópia preventiva da restauração é sem senha e a tela informa isso.

“Última cópia” no HTML registra a ação de exportar, não confirma que o arquivo foi guardado. No Android, verificar resultado do seletor/stream antes de afirmar gravação concluída. O download físico ainda precisa ser validado no aparelho.

Falha de gravação não pode mostrar “Salvo”; falha de leitura não pode trocar dados por exemplos. Atualização exige migração versionada, nunca limpeza do banco.

## R10 — Interface e limites do produto

Quatro abas: Hoje, Histórico, Relatórios e Ajustes. Contas do mês abre pelo Histórico/Ajustes. Anotar frete é a ação principal. Ícones com texto, valores grandes, toque de pelo menos 48 dp e teste com letras ampliadas. Preservar estrada/Fiorino e respeitar redução de movimento.

Português simples; sem mensagens de IA ou julgamento de resultado. Sem cadastro, publicidade, assinatura, serviços pagos ou permissões de localização, contatos, SMS e banco. Dados reais e assinatura fora do GitHub.

Android nativo planejado; não trocar por WebView silenciosamente. CSV/PDF, manutenção por km, biometria e sincronização são futuras, não funções prontas.
