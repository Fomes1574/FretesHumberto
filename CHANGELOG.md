# Histórico de alterações

## 30/10/2026 (tarde) — App Android: banco, telas, contas e cópia

- **Banco (etapa 2):** Room versão 1 com esquema exportado e conferido pelo CI. Lançamento numa tabela só, sem coluna que ligue gasto a frete. Ocorrência única por conta+mês e índice único em `lancamento.ocorrencia`. `Repositorio` grava cada operação numa transação, só com o que mudou; falha desfaz tudo e a tela não mostra "Salvo".
- **Telas (etapa 3):** Hoje, Histórico, Relatórios e Ajustes em Compose; formulários de frete, abastecimento e despesa; calculadora isolada; estrada da Fiorino em Canvas; gráficos de lucro por dia e combustível em 6 meses; resumo para o WhatsApp; letra A/A+/A++.
- **Contas do mês (etapa 4):** cadastrar, só este mês, deste mês em diante, pular/retomar e pausar; WorkManager a cada 12 h e a abertura do app usam a mesma geração idempotente.
- **Cópia (etapa 5):** arquivo `.fretes` versão 1 pelo seletor do Android, senha opcional (AES-256-GCM, PBKDF2-SHA256 210 mil), gravação conferida lendo de volta, restauração validada e transacional, cópia interna dos dados anteriores para poder voltar, lembrete semanal.
- **Testes:** 66 no `:core` (27 novos) e 17 no `:app` (11 do repositório com Room em memória, 6 de fluxos de tela), com Robolectric. Verdes no GitHub Actions. **Nenhum teste em aparelho ou emulador.**
- ViewModel criado por fábrica explícita com a `Application` da Activity (a fábrica padrão guardava a primeira `Application` numa variável estática e os testes de tela viam o banco de outro teste).
- DataStore criado por instância da `Application`; agendador não derruba o app se falhar.

## 30/10/2026 — Início do Android

- Decisões técnicas fechadas: minSdk 26, target/compile 36, `applicationId` `io.github.fomes1574.freteshumberto`, backup automático do Google ligado, Android começa do zero (sem importar o protótipo), valor do lucro aparece na hora.
- Projeto Gradle com módulos `:core` (Kotlin puro) e `:app` (Compose), catálogo de versões e wrapper do Gradle 9.3.0.
- Regras portadas para Kotlin em `core/`: dinheiro, datas, resumo, metas, calculadora, combustível, preço de referência, contas do mês, diário (salvar/apagar/desfazer/conferir) e estrada da meta. 39 testes passando.
- Esqueleto do app: quatro abas, tema bege/verde, fonte Atkinson Hyperlegible, ícone adaptativo com a Fiorino, regras de backup.
- GitHub Actions: testes do protótipo e do Android, APK debug a cada push, APK release assinado por tag. Dependabot mensal.
- `CLAUDE.md`, comandos `/proxima-etapa` e `/conferir`, guia da chave de assinatura e pesquisa de melhorias em outros apps.
- Corrigida contradição na ARQUITETURA.md: acima da meta o valor final aparece na hora (sem contagem).
- Primeiro build no GitHub Actions verde: testes do `:core` no Gradle e APK debug gerado. Ainda não aberto num aparelho.
- Ícone redesenhado: Fiorino branca em movimento sobre estrada, com sol e céu verde; versão temática (monocromática) para Android 13+. Prévia em `docs/img/13-icone.png`.

## 30/10/2026 — Documentação e entrega no repositório

- Organizada a documentação de objetivo, regras, arquitetura, decisões, testes e roteiro Android.
- Adicionado `AGENTS.md` para orientar a IA de programação e evitar regressões de escopo.
- Adicionado MD completo gerado a partir das fontes documentais.
- Preparados protótipo, fontes, licenças e testes para a primeira entrega ao GitHub; nenhum módulo Android foi criado.
- Explicitadas limitações: localStorage, execução de recorrência na abertura, download físico não confirmado, ausência de testes Android.
- Separado modelo Android proposto de código já existente. Retiradas referências locais de evidência que não estariam disponíveis no repositório.

## 29/10/2026 — Atualização do protótipo

- Corrigida interpretação de dinheiro com milhares e centavos e preservação de duração em minutos.
- Fretes, abastecimentos e despesas independentes, com exclusão e edição sem cascata.
- Calculadora isolada do diário, com sobra, custo estimado e rendimentos numéricos.
- Relatórios com semana segunda–domingo, período personalizado de até seis meses e dois dias específicos.
- Metas diárias com vigência, mensal por mês e comparações de períodos equivalentes.
- Contas mensais com versão, exceção, pausa, mês pulado e vencimento ajustado.
- Validação de estado, migração do protótipo anterior, cópia com senha opcional e restauração.
- Referência de preço por data financeira, aviso de erro de gravação e preservação de leitura inválida.
- Gráficos, letra ampliada e fontes incorporadas preservando o visual de referência.
- 32 testes automatizados e revisão de interface no navegador. Sem teste no Android físico ou APK.

As imagens em `docs/img/` são referências originais anteriores à correção. Não representam todos os fluxos atualizados.
