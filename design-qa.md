# Validação do protótipo — 29/10/2026

## Escopo e resultado

Protótipo HTML atualizado, mantendo o visual original. **32 testes automatizados passaram, sem falhas.** Os fluxos abaixo foram exercitados no navegador com dados fictícios. A inspeção visual passou nas superfícies verificadas; não é certificação do aplicativo Android, que ainda não existe nesta entrega.

O salvamento físico do arquivo de backup pelo navegador permanece sem confirmação neste ambiente. A geração, criptografia, integridade, compatibilidade entre implementações e leitura/restauração foram verificadas. O evento de download da automação expirou; isso não comprova nem sucesso nem falha de salvamento no aparelho. Não há aprovação completa do ciclo de backup em Android até o teste no celular.

## Referência visual

Fonte: `src/template-original.html` e imagens originais em `docs/img/`, recebidas no material do projeto. Referência e versão atual foram abertas no mesmo navegador, com viewport externo 1363 × 936 e apresentação em iframe de 390 px. A área útil interna tem 375 px devido à barra de rolagem do navegador de teste.

Comparação lado a lado da tela Hoje: mesmos fundo, fonte Atkinson Hyperlegible, cores, cartões, hierarquia, ícones, estrada/Fiorino e navegação. O botão da calculadora passou a se chamar “Calcular frete”. O lucro final aparece imediatamente, preservando a animação da estrada. Mensagens e novos formulários usam a linguagem e os componentes existentes.

A comparação visual foi realizada na sessão de 29/10/2026. As capturas de inspeção interna não fazem parte deste repositório. As imagens em `docs/img/` continuam como referências originais, anteriores às correções, e não devem ser confundidas com capturas dos fluxos novos. Uma primeira captura estava com pintura anterior da tela e foi descartada antes da conferência final.

## Verificações na interface

| Fluxo | Resultado observado |
|---|---|
| Calculadora | R$ 80, 40 km, 8 km/l, R$ 4/l, 90 min → combustível R$ 20, sobra R$ 60, R$ 1,50/km e R$ 40/h; fechar não alterou o diário |
| Entrada e edição | Colar `R$ 1.234,56` salvou corretamente; reabrir frete de 17 minutos manteve 17 |
| Histórico antigo | Mudar frete para 01/01/2020 atualizou origem e destino e manteve o registro editável |
| Dois dias | 27/10 e 29/10 somaram R$ 722,60, sem incluir 28/10 |
| Intervalo | 01/01–01/07 foi rejeitado; 01/10–29/10 foi aceito com R$ 8.715,20 |
| Meta | Mudar de R$ 250 para R$ 300 alterou somente a vigência de hoje em diante; a conta anterior conservou sua meta |
| Conta mensal | Conta de R$ 100 no dia 31: setembro em 30/09 lançada, outubro em 31/10 prevista; reabrir não duplicou |
| Restauração com senha | Fixture de três lançamentos validada e restaurada: R$ 285 − R$ 40 − R$ 20 = R$ 225; faltam R$ 25 |
| Exclusão independente | Apagar o frete restaurado manteve R$ 40 de combustível e R$ 20 de despesa, resultado −R$ 60; Desfazer foi acionado |
| Gráfico de combustível | Lista e gráfico dos seis meses do relatório mensal mostraram agosto R$ 170, setembro R$ 1.270, outubro parcial R$ 1.410 e meses anteriores sem gastos |
| Fonte e navegação | Hoje normal e A++ (23 px), sem overflow horizontal e com navegação visível; dimensões da página iguais à área útil |
| Console | Sem erro de origem do app nas observações finais; houve erro da extensão do navegador, fora do código do protótipo |

Ao terminar, os dados fictícios originais foram recarregados e a apresentação voltou à tela Hoje, em letra normal.

## Testes automatizados

Comando: `npm test` (`node --test tests/*.test.cjs`). Resultado: 32 aprovados, zero falhas. Cobrem interpretação monetária, centavos, cálculo de sobra e combustível, guardas contra divisão por zero, metas históricas, semana, limite de intervalo, dois dias isolados, movimentação/exclusão independente, desfazer sem duplicar, duração, revisão de dia, referência de preço, comparação equivalente, recorrência e dias ajustados, migração e validação de estado, cópias com e sem senha, senha incorreta, adulteração e interoperabilidade WebCrypto/implementação incorporada.

O HTML gerado também passou em `node --check src/prototipo-check.js`.

## Limites e próximo teste

- Nenhum teste em celular Android físico, APK, Room ou WorkManager foi realizado.
- A++ verificado no navegador não substitui teste da escala de fonte e do teclado do Android.
- Cópia local precisa de teste completo no aparelho: salvar, encontrar o arquivo e restaurar, inclusive com senha incorreta e arquivo inválido.
- Recursos incorporados eliminam dependências de rede no HTML, mas o uso em modo avião ainda deve ser conferido no aparelho escolhido.
- A recorrência é conferida ao abrir/retomar a página, não em segundo plano com o navegador fechado.
- A cena original foi preservada. Cruzamento de meta, confete, redução de movimento e toque rápido devem integrar o teste com o motorista e a implementação Android.
- Biometria e exportação CSV/PDF continuam previstas, sem botões simulando funcionamento.

Na sessão de 29/10 não houve publicação nem alteração do repositório remoto. A entrega documental de 30/10 inclui as fontes do protótipo no GitHub, sem publicar um site ou produzir APK. Não foi contratado serviço pago.
