# Estado atual e mapa do código

Revisão: 30/10/2026. Protótipo web de 29/10/2026 + **início do Android em 30/10/2026**: módulo `:core` com as regras em Kotlin (39 testes passando) e esqueleto do `:app` (Compose, quatro abas, tema, ícone, backup). O `:app` ainda não foi compilado; a primeira compilação acontece no GitHub Actions ou no Android Studio.

## Implementado versus previsto

| Área | Protótipo atual | Android |
|---|---|---|
| Hoje, Histórico, Relatórios, Ajustes | Implementados em HTML/JS | Esqueleto: abas, tema e cartão do lucro; telas pendentes |
| Fretes, abastecimentos e despesas independentes | Implementados | Regras em `core/Diario.kt` e `Calculos.kt` (testadas); banco pendente |
| Calculadora isolada | Implementada, sem gravação no diário | `core/Calculos.kt` (testada); tela pendente |
| Metas por vigência/mês | Implementadas | `core/Modelo.kt` (testadas); persistência pendente |
| Dia, semana, mês, personalizado e dois dias | Implementados | `core/Datas.kt` e `Relatorio.resumir` (testados); telas pendentes |
| Recorrências com versões, exceções e mês pulado | Implementadas ao abrir/retomar | `core/Recorrencias.kt` (testada); WorkManager e Room pendentes |
| Gráficos e estrada/Fiorino | SVG/JS, preservando a referência visual | Regras em `core/Estrada.kt` (testadas); Canvas pendente |
| Conferência e desfazer exclusão | Implementados | Pendente |
| Cópia validada e senha opcional | Implementadas; salvamento físico não confirmado na automação | Seletor de arquivos e recuperação pendentes |
| Armazenamento | `localStorage` do navegador | Room e DataStore no catálogo de versões; backup do Google configurado no manifesto |
| A/A+/A++ | Implementados; inspeção em largura de celular | Escala Android/TalkBack/teclado pendentes |
| CSV/PDF, biometria, manutenção por km | Não implementados | Melhorias futuras |
| APK, assinatura, CI de build, atualização instalada | Não existem | Workflow e assinatura por Secrets preparados; nenhum APK gerado ainda |
| Backend, login, IA, integração Lalamove | Fora do escopo | Não implementar sem mudança de escopo |

## Fonte do protótipo

| Arquivo | Responsabilidade e pontos de entrada |
|---|---|
| `src/core.js` | IIFE `Core`, funções puras de estado, dinheiro, datas, metas e recorrência |
| `src/app.js` | `load`, `commit`, telas `vHoje/vHist/vRel/vAj/vAccounts`, formulários e eventos |
| `src/build.py` | Lê template, CSS incorporado, bundle e fontes; gera HTML e JS temporário de checagem |
| `src/template-original.html` | Fonte visual original: CSS, ícones `I`, funções da estrada e `seed()`; não usar seu diário antigo |
| `src/fonts.css` | Atkinson Hyperlegible incorporada, sem consulta à rede |
| `src/backup-crypto.js` | Alternativa a WebCrypto, usando noble AES-GCM e PBKDF2 |
| `src/backup-crypto.bundle.js` | Bundle já incorporado à distribuição |
| `docs/prototipo.html` | Saída completa para abrir; não é a fonte principal a editar |
| `docs/mobile.html` | Wrapper visual de 390 px, não emulador |
| `serve.mjs` | Servidor local de desenvolvimento, servindo apenas `docs/` |
| `tests/regras.test.cjs` | 32 testes Node; não são testes de tela ou Android |

O arquivo-fonte usa funções compactas. Localize pelo nome com busca, não por número de linha fixo.

## Mapa de funções

| Necessidade | Funções |
|---|---|
| Converter número, centavos, arredondar | `Core.parseScaled`, `Core.num`, `Core.integer`, `Core.roundDiv` |
| Datas e limites | `Core.validDate`, `addD`, `addM`, `monthRange`, `weekStart`, `validRange`, `previousRange` |
| Conta do diário | `Core.summary` |
| Metas | `Core.dayGoal`, `monthGoal`, `setDayGoal` |
| Simulação | `Core.calc`; UI `calcSheet`, `calcData`, `calcUpd` |
| Abastecimento e preço de referência | `Core.fuel`, `Core.price`; UI `abCalc`, `abUpd` |
| Salvar, remover e desfazer | `Core.saveEntry`, `removeEntry`, `restoreEntry`, `touch`; UI `submitEntry` e `commit` |
| Recorrência | `Core.recVersion`, `dueDate`, `generate`; UI `submitRec`, `reconcileRec`, `recSkip`, `recToggle` |
| Formato/validação do estado | `Core.defaults`, `Core.normalize` |
| Cópias | `encodeBackup`, `decodeBackup`, `backupReady`, `restoreSheet`, `finishRestore` |
| Data virando e despesas devidas | `rollDate`, `processRecurring`, eventos de foco/visibilidade |
| Gráficos | `barChart`, `fuelMonths`; cena original extraída pelo gerador |

## Estado e persistência atuais

Chave do navegador: **`meusfretes_proto_v2`**, preservada para compatibilidade, apesar de o estado normalizado ser **versão 3**. O nome da chave não é a versão do formato de exportação.

| Campo de estado | Conteúdo |
|---|---|
| `version` | Versão do estado: 3 |
| `settings` | Consumo, preço padrão/manual, tamanho de letra e data da exportação; metas antigas também preservadas para migração |
| `lanc` | Lançamentos: `viagem`, `abast`, `despesa`; cada registro independente |
| `goalsDay` | Metas diárias ordenadas por `desde` |
| `goalsMonth`, `monthBase` | Metas específicas por mês e base dos meses sem definição explícita |
| `rec` | Regras recorrentes com versões mensais |
| `occ` | Ocorrências por chave `recId|AAAA-MM`, status e alteração individual opcional |
| `revisions`, `checked` | Revisão de cada dia e referência usada em sua conferência |
| `demo` | Identifica uso de exemplos fictícios |

`valor`, `preco` e metas são centavos. `litros100`, `km100` e `kml100`, quando usados, representam centésimos da unidade indicada. Os detalhes opcionais `lanc.km` e a configuração `settings.kml` ainda são números decimais JavaScript; não confundir com centavos ou copiar o formato sem análise para Kotlin. `lanc.min` é minuto inteiro.

Fluxo de gravação: clonar estado → aplicar ação → normalizar/validar → persistir → assumir novo estado. Em falha, não dar sucesso. Leitura inválida preserva os dados brutos e oferece restauração, sem substituir por exemplos.

A UI mantém rascunhos de formulários de lançamento em memória ao fechar sem salvar; eles não são persistidos após fechar/recarregar o navegador. A calculadora mantém somente seu estado transitório.

## Formato da cópia

Envelope JSON com `format: FretesHumberto`, `formatVersion: 1` e `encrypted`. Sem senha, há `payload` com o estado. Com senha, há metadados da proteção, salt, IV e ciphertext em base64. **Versão do envelope 1 e versão do estado 3 são coisas distintas.**

A implementação protegida usa AES-GCM, PBKDF2-SHA256/210000, chave de 256 bits, salt 16 bytes, IV 12 bytes e dado autenticado `FretesHumberto:1`. Native WebCrypto e bundle incorporado foram testados entre si. O Android precisará de implementação equivalente ou migração explícita do formato; não declarar compatibilidade antes de testar vetores.

A fixture `tests/fixtures/restauracao-teste.fretes` contém somente dados fictícios; senha `teste-prototipo-2026`. Seu conteúdo financeiro é frete R$ 285, abastecimento R$ 40 e despesa R$ 20 em 29/10/2026. Essa data fixa não acompanha o dia atual.

## Limites importantes para a próxima IA

- Browser localStorage não é um banco Android nem mecanismo de sincronização entre abas/aparelhos. Conflitos de escrita entre duas abas não foram resolvidos.
- Recorrência não roda com navegador fechado. Gerar despesa não paga banco.
- Download do backup: geração e restauração foram testadas; gravação física do arquivo não foi confirmada pela automação.
- O lucro final aparece imediatamente; a cena da estrada continua animada. Não reintroduzir contagem numérica que dificulte conferir o saldo só para igualar uma imagem antiga.
- O gráfico atual mostra uma linha de meta quando todas as metas do intervalo são iguais. Representar mudanças de meta dentro do gráfico é refinamento ainda pendente; os totais usam as vigências corretas.
- Relatórios continuam mostrando km anotados como referência, sem converter isso em lucro/km de uma amostra parcial.
- Modelo Room, política de backup do Android, assinatura, permissão de arquivos, teclado e acessibilidade física ainda precisam ser implementados e testados.
- Limites atuais de validação incluem 50 mil lançamentos, 200 recorrências e arquivo de até 10 milhões de caracteres no decodificador; a seleção limita a 10 MB. São guardas do protótipo, não política de apagar histórico. Há também guarda de 2400 iterações por recorrência. Revisar esses limites ao implementar armazenamento durável.

O estado atual serve de referência de comportamento e de teste, não de justificativa para manter limitações acidentais no aplicativo definitivo.
