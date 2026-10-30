# Instruções para agentes de programação

## Situação e leitura

Protótipo HTML funcional e Android em construção (desde 30/10/2026): módulo `:core` com as regras em Kotlin e testes, esqueleto `:app` em Compose, wrapper do Gradle e workflow de CI. Ainda não há telas completas, banco Room ou APK publicado. Não descreva decisões planejadas como funções entregues.

Leia: `CLAUDE.md` (atalhos), `README.md`, `docs/REGRAS_DE_NEGOCIO.md`, `docs/ESTADO_ATUAL.md`, `docs/ARQUITETURA.md`, `docs/PLANO_ANDROID.md` e `docs/TESTES.md`. `docs/CONTEXTO_COMPLETO.md` reúne esses documentos automaticamente; não é uma segunda especificação para editar à mão.

Instruções explícitas do usuário prevalecem. Para decisões de produto, use as regras e o registro de decisões; para funcionamento implementado, confira código e testes. Divergência do código é pendência, não autorização para alterar a regra. Imagens antigas e o template original não substituem os requisitos.

## Não regredir

1. Fretes, despesas e abastecimentos são independentes. Nenhuma despesa tem vínculo financeiro com frete. Excluir/mover frete só afeta ele e seus detalhes técnicos.
2. Combustível sai do diário pelos abastecimentos cadastrados. A estimativa fica só na calculadora isolada, que não grava lançamentos.
3. Não reintroduzir recebimento pendente, CDB/investimentos ou classificação de rentabilidade.
4. Datas financeiras locais sem horário. Semana segunda inclusiva até a próxima segunda exclusiva. Sem relatórios por hora; duração opcional em minutos continua permitida.
5. Filtro de seis meses não apaga histórico nem impede editar lançamentos antigos. Conferir dia não bloqueia edição.
6. Preservar metas históricas. Meta mensal pertence ao mês de referência.
7. Recorrência idempotente, única por conta+mês. Respeitar mês pulado, versões e exceções. Fevereiro usa dia 28 para vencimentos 29–31, inclusive bissexto: é decisão deliberada.
8. Não adicionar backend, login, analytics, IA no produto, API paga ou dependência de internet. Linguagem curta, comum e centrada nos valores.
9. Nunca versionar dados financeiros reais, cópias pessoais, chaves ou credenciais. A fixture existente é fictícia e documentada.
10. Sem migração destrutiva, limpeza de banco, troca silenciosa por exemplos ou mensagem “Salvo” depois de falha.

## Onde editar

- `src/core.js`: regras puras; centavos inteiros, IDs estáveis e prejuízo preservado.
- `src/app.js`: UI, persistência e cópias; validar e persistir antes de assumir sucesso.
- `src/template-original.html`: fonte de CSS, ícones, cena e seed extraídos por `src/build.py`. Não restaurar sua lógica antiga na aplicação atual.
- `docs/prototipo.html`: saída gerada. Não corrigir só esse arquivo; regenerar após editar fontes.
- `src/backup-crypto.bundle.js`: bundle gerado; manter fonte e lockfile coerentes ao alterar dependências.
- `src/prototipo-check.js`: temporário para checar sintaxe, não versionar.
- `docs/img/`: referências originais, não capturas dos novos fluxos.
- `core/`: regras em Kotlin puro, espelho de `src/core.js`. Mudou uma regra? Mude e teste nos dois, ou registre a divergência.
- `app/`: Android. Só UI, ViewModels, Room e integrações; nada de cálculo financeiro aqui.

Preservar Atkinson Hyperlegible, bege/verde, quatro abas e estrada/Fiorino. Ajustes de legibilidade são permitidos; nova direção visual depende de pedido do usuário.

## Validação e entrega

Após alterar regras/código: `npm test`. Após alterar fontes do HTML: `npm run build:html` e `node --check src/prototipo-check.js`. Para UI, conferir fluxos afetados em largura de celular com letra normal e A++. Teste Node não comprova acessibilidade, download ou funcionamento nativo Android.

Para documentação, conferir links locais, coerência entre implementado/previsto e executar `python3 scripts/build_context.py`. Não inventar APK, meses de uso real, testes no celular ou confirmação de download.

Antes de programar Android, seguir `docs/PLANO_ANDROID.md` (próxima tarefa = primeira caixa `[ ]`). Regras novas ou alteradas vão primeiro para `core/` com teste. Portar o comportamento, não a estrutura de localStorage. Regras financeiras fora da UI; transações e migrações Room; mesmos casos de referência em Kotlin. O modelo Android do documento é proposta, não migração pronta.

Na entrega, relatar mudanças, testes realmente executados, limitações e pendências. Modificar documentos/protótipo não equivale a concluir o aplicativo Android.
