# Melhorias inspiradas em outros apps

Pesquisa de 30/10/2026 em apps de controle de veículo e de ganhos de motorista (Fuelio, Drivvo, For Driver, Combustível & Custos e listas de apps para motoristas e motoboys). Objetivo: aproveitar o que funciona para quem dirige o dia todo, **sem** trazer o que contraria as regras do projeto (internet obrigatória, conta, anúncios, julgamento de "compensa ou não").

**Nada aqui entra no escopo sozinho.** A v1 é a do [PLANO_ANDROID.md](PLANO_ANDROID.md). Cada item abaixo precisa de "pode fazer" do usuário antes de ser implementado; quando aprovado, mova para a tabela de Próximas etapas da [ARQUITETURA.md](ARQUITETURA.md).

## Recomendadas (baixo custo, alto proveito)

| # | Melhoria | Onde aparece | Como seria no Meus Fretes | Custo técnico | Versão sugerida |
|---|---|---|---|---|---|
| 1 | **Atalhos no ícone** (segurar o ícone → "Anotar frete", "Abasteci") | Fuelio (atalho para abastecimento rápido) | `shortcuts.xml` estático abrindo o formulário direto | Muito baixo, sem permissão | v1 (pode entrar junto da etapa 3) |
| 2 | **Widget na tela inicial** com o lucro de hoje, a meta e botões Frete / Abasteci | Fuelio (widgets de ações rápidas e painel) | Jetpack Glance lendo o mesmo repositório; toque abre o formulário | Médio (nova dependência Glance) | v1.1 |
| 3 | **Lembrete diário para anotar** ("Já anotou os fretes de hoje?"), num horário que ele escolhe, desligável | Lembretes do Fuelio e do Drivvo | Notificação local via WorkManager; aparece só se o dia ainda está vazio. Pede `POST_NOTIFICATIONS` no Android 13+ | Baixo | v1.1 |
| 4 | **Busca no Histórico** por descrição/cliente ("Mudança", nome do cliente) | Fuelio (busca nos registros) | Campo de busca simples sobre a lista de dias | Baixo | v1.1 |
| 5 | **Lembretes de manutenção por data ou km**: troca de óleo, pneus, alinhamento, licenciamento | Drivvo e Fuelio (lembretes por data ou hodômetro); Combustível & Custos | Lista "Manutenção" com próxima data/km; avisa pelo mesmo mecanismo do item 3. Já estava previsto como "manutenção por km" | Médio | v2 |
| 6 | **Consumo real (km/l) pelo método "tanque cheio"** | Fuelio (algoritmo de tanque cheio) | No Abasteci, dois campos opcionais: hodômetro e "Enchi o tanque?". Com dois tanques cheios seguidos o app calcula o km/l real e sugere usar na calculadora | Médio | v2 |
| 7 | **Exportar o mês em CSV/PDF** para mandar ao filho ou abrir no Excel | Fuelio (exportação CSV) | Já previsto. CSV primeiro (mais simples), PDF depois | Baixo/médio | v2 |
| 8 | **Contas anuais** (IPVA, licenciamento, seguro anual) | Drivvo (lembretes de impostos, multas e financiamento) | Repetição "anual" nas Contas do mês, com a mesma regra idempotente | Baixo | v2 |

## Avaliadas e descartadas

| Ideia | Onde aparece | Por que não |
|---|---|---|
| Registro automático de km por GPS | Fuelio (Trip Log) | Pede localização o tempo todo e gasta bateria; contraria "sem acesso à localização" |
| Preço de combustível e postos próximos | Fuelio, Drivvo | Depende de internet e serviço externo; o preço é o que ele digita |
| Vários veículos e motoristas, permissões de frota | Drivvo | Um motorista, uma Fiorino: só complica a tela |
| Sincronização em nuvem / login | Drivvo, Fuelio | Fora do escopo (DECISOES.md); o backup do Google + cópia manual cobrem a troca de celular |
| Dois tanques / GNV | Drivvo, Fuel Manager | Não é o caso da Fiorino dele; dá para rever se ele instalar GNV |
| Assinatura PRO, anúncios | Drivvo, Fuelio | Custo zero e sem anúncios é regra do projeto |
| Classificar fretes em "bom/ruim" | Vários apps de motorista | Proibido pela regra R05: só números |

## Fontes

- Fuelio (descrição e notas de versão): widgets de ações rápidas, atalho para abastecimento, busca nos registros, lembretes por data/hodômetro, algoritmo de tanque cheio, exportação CSV, backup no Google Drive — [APKMirror](https://www.apkmirror.com/apk/sygic/fuelio-gas-log-costs), [fileplanet](https://fuelio.fileplanet.com/apk), [App Pricing Lab](https://apppricinglab.com/app/google_play/com.kajda.fuelio)
- Drivvo: lembretes de manutenção, impostos, multas e financiamento por data ou quilometragem; receitas e despesas do motorista — [BlueStacks](https://www.bluestacks.com/campaign/br.com.ctncardoso.ctncar/es), [Androidayuda](https://en.androidayuda.com/control-car-expenses-application/)
- For Driver, Combustível & Custos e outros: metas de ganho, rendimento mensal, alertas de manutenção — [Motonline](https://motonline.com.br/noticia/5-aplicativos-gratis-motoboy/)
- Separar gastos do trabalho e pessoais, registrar até gastos pequenos — [Gringo](https://gringo.com.vc/controle-financeiro-para-motorista)
