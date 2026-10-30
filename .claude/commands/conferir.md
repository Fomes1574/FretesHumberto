---
description: Roda todos os testes e confere se o projeto continua coerente
---
Rode, nesta ordem, e relate o resultado de cada um sem esconder falhas:

1. `npm test` (protótipo web)
2. `./gradlew :core:test`
3. `./gradlew :app:testDebugUnitTest :app:assembleDebug`
4. Verifique se algum arquivo alterado contradiz `AGENTS.md` (as 10 regras de "Não regredir") ou `docs/DECISOES.md`.
5. Verifique se não há segredos, `.jks`, `keystore.properties` ou dados reais prontos para commit (`git status`).

Termine com uma lista curta: o que passou, o que falhou e o que não pôde ser verificado.
