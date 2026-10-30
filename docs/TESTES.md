# Testes e critérios de aceite

Separar sempre: testes automatizados das regras JavaScript, revisão histórica da interface no navegador e testes futuros do Android.

## Resultado automatizado

Comando: `npm test`. Suíte: `tests/regras.test.cjs`, executada com o runner do Node. **32 testes aprovados em 29/10/2026 e reexecutados em 30/10/2026; nenhuma falha.** Não exige instalação de pacotes, pois o bundle já está incluído.

| Teste | Verificação |
|---|---|
| 1 | Dinheiro brasileiro colado, separadores, truncamento e rejeição de ambiguidade |
| 2 | Centavos e limite numérico da entrada |
| 3 | R$ 285 − R$ 40 − R$ 20 = R$ 225, faltando R$ 25 |
| 4 | Simulação: R$ 60 de sobra, R$ 1,50/km e R$ 40/h |
| 5 | Resultado negativo e ausência de indicador por hora sem tempo; cálculo puro |
| 6 | Zero e valores inválidos no simulador |
| 7 | Abastecimento por preço ou litros |
| 8 | Vigência das metas diárias |
| 9 | Meta mensal só no mês selecionado |
| 10 | Meta histórica não muda com configuração atual |
| 11 | Semana segunda–domingo, excluindo próxima segunda |
| 12 | Intervalo de seis meses, ordem das datas e futuro |
| 13 | Edição de registro antigo sem expiração |
| 14 | Dois dias sem incluir intermediários |
| 15 | Excluir/mover frete deixa gastos intactos |
| 16 | Desfazer não duplica registro |
| 17 | Duração de 17 minutos preservada |
| 18 | Revisão do dia muda após edição |
| 19 | Preço de abastecimento antigo não substitui o mais recente |
| 20 | Comparação da semana parcial com mesmo recorte |
| 21 | Comparação mensal respeita o último dia disponível |
| 22 | Ajuste 31→28→31→30 e fevereiro bissexto no dia 28 |
| 23 | Geração de recorrência idempotente e somente quando devida |
| 24 | Excluir ocorrência pula o mês e não recria |
| 25 | Pausa preserva lançamentos anteriores |
| 26 | Data final da recorrência |
| 27 | Rejeição de estado inválido, data impossível, IDs duplicados e versão incompatível |
| 28 | Migração de litros, metas e duração do protótipo anterior |
| 29 | Cópia sem senha preserva estado e recorrência |
| 30 | Cópia protegida, senha incorreta e adulteração |
| 31 | Verificação estática de ausência do antigo caminho de salvar calculadora e de fórmulas indevidas |
| 32 | Compatibilidade de cópia protegida entre WebCrypto e alternativa incorporada; senha incorreta |

A suíte não é prova de todos os fluxos de UI nem cobertura completa de todas as entradas. O teste 31 inspeciona texto-fonte, não navega na interface. Não apresentar “32 testes” como teste de meses em produção.

## Checagem da distribuição

```sh
npm run build:html
node --check src/prototipo-check.js
```

O HTML precisa ser reproduzível a partir das fontes. O JS de checagem é temporário. Nesta entrega, nenhum comportamento novo foi adicionado ao aplicativo: o trabalho de 30/10 concentrou documentação e organização para o GitHub.

## Revisão de interface de 29/10/2026

Registrada em [design-qa.md](../design-qa.md). Incluiu: valor colado R$ 1.234,56; duração de 17 minutos; mudança de frete para 2020; dois dias isolados; rejeição de intervalo maior que seis meses; vigência da meta; recorrência sem duplicar ao reabrir; calculadora sem gravar; restauração protegida; exclusão mantendo gastos; letra A++ e largura de celular.

É evidência histórica de navegador. Não foi repetida uma sessão inteira de UI em 30/10, pois não houve alteração funcional. Não há capturas novas do app nativo nem aparelho Android certificado.

A fixture fictícia de restauração está em `tests/fixtures/restauracao-teste.fretes`, senha `teste-prototipo-2026`. Tem lançamentos em **29/10/2026**: após restaurar em outro dia, consultar aquela data para conferir R$ 225. Não esperar que a fixture apareça automaticamente no dia atual.

## Pendências obrigatórias no Android

- [ ] Portar os casos de referência e validar dinheiro sem ponto flutuante/overflow.
- [ ] Testar transações, falhas de disco e migrações sem perda de dados.
- [ ] Testar edição/deleção independente e desfazer com estado atualizado.
- [ ] Worker e abertura simultâneos sem despesa duplicada; meses pulados persistem.
- [ ] Executar viradas de dia/semana/mês e troca de fuso com relógio controlado.
- [ ] Testar filtros em meses de duração diferente, dias 29–31 e dados de anos anteriores.
- [ ] Testar exportar, localizar e restaurar o arquivo físico com/sem senha; cancelamento, senha incorreta e arquivo inválido.
- [ ] Conferir restauração de metas, versões de recorrência, exceções e conferências, não só fretes.
- [ ] Atualizar APK por cima com a mesma assinatura, mantendo dados.
- [ ] Modo avião, processo encerrado, aparelho reiniciado e período sem abrir o app.
- [ ] Letra A++, escala Android, TalkBack, teclado aberto e área de toque.
- [ ] Cena: cruzar meta, ficar acima, recuar abaixo, troca de aba e redução de movimento.
- [ ] Desempenho com histórico maior e ausência de bloqueio na interface.
- [ ] Piloto com o motorista, registrando dificuldades observadas.

## Como registrar próximos resultados

Informar data, commit, comando ou fluxo, esperado, observado, aparelho/ambiente e limitações. Distinguir falha do produto de limitação de teste. Não afirmar salvamento de arquivo só porque foi gerado um Blob, nem funcionamento nativo porque o teste Node passou.

## Testes Kotlin do módulo `:core` (30/10/2026)

`core/src/test/kotlin/.../RegrasTest.kt` repete os casos do protótipo em Kotlin e acrescenta casos de formatação, preço médio, dia só com gastos, alteração "só deste mês" e regras da estrada: **39 testes, todos passando**. Foram compilados e executados com o compilador Kotlin 2.0.21 embutido no Gradle, fora do Gradle (o ambiente não tinha acesso ao Maven). Confirmado depois no GitHub Actions com Kotlin 2.3.20 (`./gradlew :core:test`, PR #1), junto com a compilação do `:app` e o APK debug.

Não foram executados: testes instrumentados, emulador ou aparelho físico.

