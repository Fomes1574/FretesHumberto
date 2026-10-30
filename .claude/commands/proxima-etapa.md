---
description: Faz a próxima tarefa pendente do plano Android, com testes e registro
---
1. Abra `docs/PLANO_ANDROID.md` e encontre a primeira caixa `[ ]` não marcada. Diga em uma frase qual é.
2. Leia as regras que ela toca em `docs/REGRAS_DE_NEGOCIO.md` e o código existente em `core/` e `app/`.
3. Escreva os testes primeiro (JUnit em `core/src/test` para regra; `app/src/test` para ViewModel/repositório).
4. Implemente o mínimo para a tarefa, seguindo as convenções do CLAUDE.md.
5. Rode `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug`. Se falhar, corrija. Se não der para rodar (sem rede/SDK), diga exatamente o que não foi verificado.
6. Marque `[x]` no PLANO_ANDROID.md, atualize `docs/ESTADO_ATUAL.md` e `CHANGELOG.md`, rode `python3 scripts/build_context.py`.
7. Resuma: o que mudou, testes executados e o resultado, e qual é a próxima caixa.

$ARGUMENTS
