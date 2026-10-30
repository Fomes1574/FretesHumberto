# Meus Fretes — FretesHumberto

Controle simples de fretes, abastecimentos, despesas e metas para um motorista Android com pouca familiaridade com tecnologia. O foco é anotar os valores e ver quanto sobrou no dia, com histórico e relatórios fáceis de consultar.

**Situação em 30/10/2026: projeto e protótipo web. O aplicativo Android ainda não foi implementado. Não há APK.** O código atual usa HTML/JavaScript e armazenamento no navegador. Kotlin, Compose, Room e WorkManager são a arquitetura prevista.

## Documentação

| Necessidade | Documento |
|---|---|
| Um único MD com o contexto inteiro | [CONTEXTO_COMPLETO.md](docs/CONTEXTO_COMPLETO.md) |
| Instruções para IA de programação | [AGENTS.md](AGENTS.md) |
| Contrato das regras financeiras | [REGRAS_DE_NEGOCIO.md](docs/REGRAS_DE_NEGOCIO.md) |
| Telas, design e arquitetura Android prevista | [ARQUITETURA.md](docs/ARQUITETURA.md) |
| O que existe e o que ainda falta | [ESTADO_ATUAL.md](docs/ESTADO_ATUAL.md) |
| Etapas de desenvolvimento Android | [PLANO_ANDROID.md](docs/PLANO_ANDROID.md) |
| Testes e limites da validação | [TESTES.md](docs/TESTES.md) e [design-qa.md](design-qa.md) |
| Decisões e alterações | [DECISOES.md](docs/DECISOES.md) e [CHANGELOG.md](CHANGELOG.md) |

## Regras essenciais

- **Lucro = fretes líquidos − abastecimentos − outras despesas**, nas datas selecionadas.
- Fretes e gastos são independentes. Apagar ou mover um frete não apaga nem move despesas.
- A calculadora só simula; seus valores não entram no diário.
- Sem contas a receber, módulo de investimentos ou julgamento “compensa/não compensa”.
- Semana de segunda a domingo. Intervalos personalizados de até seis meses por consulta, com histórico preservado e editável.
- Contas mensais com ocorrência única por conta+mês. Dia 31 ajusta para 30 quando necessário; fevereiro usa 28 para vencimentos 29–31, inclusive bissexto, conforme decisão do projeto.
- Metas diárias com vigência; meta mensal por mês escolhido. Não sobrescrever metas antigas.
- Sem cadastro, servidor, anúncios, assinatura ou API paga. Dados reais ficam no aparelho.

As regras detalhadas estão em [REGRAS_DE_NEGOCIO.md](docs/REGRAS_DE_NEGOCIO.md). “Lucro” significa o resultado dos valores cadastrados, sem estimar custos ausentes.

## Testar

Baixe o repositório e abra `docs/prototipo.html` no navegador. O arquivo incorpora fonte, desenhos e criptografia, sem carregar serviços externos. A página de arquivo do GitHub mostra o código, não executa o protótipo.

Alternativa com Node 22 ou superior:

```sh
npm run dev
```

Abra o endereço indicado pelo servidor na porta 4173. `docs/mobile.html` apresenta o protótipo em uma área de 390 px; não é um emulador Android.

Os dados iniciais são **fictícios**. “Começar do zero” permite testar sem os exemplos. Os registros ficam no `localStorage` desse navegador e podem desaparecer ao limpar seus dados. Não use o protótipo como único registro financeiro.

## Testes e geração

Node 22+ e Python 3. Não precisa instalar dependências para rodar os testes existentes ou gerar o HTML usando o bundle de criptografia já incluído:

```sh
npm test
npm run build:html
node --check src/prototipo-check.js
```

Para modificar e reconstruir a criptografia:

```sh
npm ci
npm run build:crypto
npm run build:html
npm test
```

Edite `src/` e regenere `docs/prototipo.html`. O gerador aproveita CSS, ícones, estrada e dados fictícios de `src/template-original.html`; a lógica antiga desse template não é a lógica vigente. O mapa das funções está em [ESTADO_ATUAL.md](docs/ESTADO_ATUAL.md).

## Situação dos testes

Os 32 testes automatizados passam. A interface foi revisada no navegador em 29/10/2026, incluindo restauração com senha e letra ampliada. **Nenhum teste Android físico, build de APK ou migração Room foi realizado.** O salvamento físico do arquivo de backup não foi confirmado na automação e precisa de teste completo no aparelho. A recorrência atual funciona ao abrir/retomar a página, sem execução com navegador fechado.

Não há publicação automática, workflow de build, integração com a Lalamove ou serviços de nuvem. Exportação CSV/PDF, biometria, manutenção por km e sincronização ainda não foram implementadas.

## Organização

| Caminho | Conteúdo |
|---|---|
| `docs/prototipo.html` | Protótipo atualizado |
| `src/core.js` | Cálculos, datas, metas, recorrência e validação |
| `src/app.js` | Telas, formulários, armazenamento e cópias |
| `src/build.py` | Geração do HTML final |
| `src/backup-crypto.js` e bundle | Proteção de cópia também sem WebCrypto |
| `tests/` | Testes e fixture fictícia |
| `docs/img/` | Referências visuais originais, anteriores às correções |
| `docs/LICENCAS/` | Licenças dos componentes incorporados |
| `scripts/build_context.py` | Gera o MD de contexto completo |

O objetivo continua sendo custo zero. GitHub Actions para o APK futuro depende dos limites e permissões vigentes da conta; não é uma etapa já configurada nem uma garantia de disponibilidade. O protótipo atual não depende desse serviço.
