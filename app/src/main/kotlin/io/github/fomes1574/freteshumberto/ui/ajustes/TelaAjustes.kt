package io.github.fomes1574.freteshumberto.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.Copia
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Dinheiro
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.LembreteCopia
import io.github.fomes1574.freteshumberto.data.TamanhoLetra
import io.github.fomes1574.freteshumberto.ui.comum.Botao
import io.github.fomes1574.freteshumberto.ui.comum.BotaoIcone
import io.github.fomes1574.freteshumberto.ui.comum.BotaoLinha
import io.github.fomes1574.freteshumberto.ui.comum.CampoNumero
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Erro
import io.github.fomes1574.freteshumberto.ui.comum.Par
import io.github.fomes1574.freteshumberto.ui.comum.Segmentado
import io.github.fomes1574.freteshumberto.ui.comum.TextoApagado
import io.github.fomes1574.freteshumberto.ui.comum.TituloCartao
import io.github.fomes1574.freteshumberto.ui.comum.TituloTela
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AcoesAjustes(
    val salvar: (metaDia: Long, kmPorLitro: Long, preco: Long) -> Unit,
    val mudarLetra: (TamanhoLetra) -> Unit,
    val contas: () -> Unit,
    val fazerCopia: (android.net.Uri, String?) -> Unit,
    val restaurar: (android.net.Uri) -> Unit,
    val voltarCopiaAnterior: () -> Unit,
)

@Composable
fun TelaAjustes(
    e: Estado,
    hoje: LocalDate,
    letra: TamanhoLetra,
    ultimaCopia: Instant?,
    temCopiaAnterior: Boolean,
    ocupado: Boolean,
    margens: PaddingValues,
    acoes: AcoesAjustes,
) {
    var metaDia by rememberSaveable { mutableStateOf(Formato.paraCampo(e.metas.doDia(hoje))) }
    var consumo by rememberSaveable { mutableStateOf(Formato.paraCampo(e.ajustes.kmPorLitroCentesimos)) }
    var preco by rememberSaveable { mutableStateOf(Formato.paraCampo(e.precoReferencia())) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }

    var pedirSenha by rememberSaveable { mutableStateOf(false) }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmarVolta by remember { mutableStateOf(false) }

    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) acoes.fazerCopia(uri, senha.ifEmpty { null })
        senha = ""
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) acoes.restaurar(uri)
    }

    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TituloTela("Ajustes")
        Cartao {
            CampoNumero("Meta de lucro por dia", metaDia, { metaDia = it; erro = null }, grande = true)
            TextoApagado("A meta nova vale para hoje e para os próximos dias. Os dias que já passaram continuam com a meta que tinham.")
            Par(
                { m -> CampoNumero("Consumo (km por litro)", consumo, { consumo = it; erro = null }, m, prefixo = null) },
                { m -> CampoNumero("Preço do litro", preco, { preco = it; erro = null }, m) },
            )
            TextoApagado("O preço do litro se atualiza sozinho com o abastecimento mais recente. Mudar estes valores não altera dias que já passaram.")
            Erro(erro)
            Botao("Salvar ajustes", {
                val md = Dinheiro.parse(metaDia)
                val c = Dinheiro.parse(consumo); val p = Dinheiro.parse(preco)
                when {
                    md == null || md <= 0 -> erro = "Confira a meta do dia. Ex.: 250,00."
                    c == null || c <= 0 || c > 100_000 -> erro = "Confira o consumo. Ex.: 8,5."
                    p == null || p <= 0 -> erro = "Confira o preço do litro. Ex.: 3,79."
                    else -> acoes.salvar(md, c, p)
                }
            }, ligado = !ocupado)
        }

        Cartao {
            TituloCartao("Tamanho da letra")
            Segmentado(TamanhoLetra.entries.map { it.rotulo }, letra.ordinal) { acoes.mudarLetra(TamanhoLetra.entries[it]) }
            TextoApagado("Vale junto com o tamanho de letra do Android.")
        }

        Cartao {
            TituloCartao("Contas do mês")
            TextoApagado("Despesas que se repetem todo mês, como aluguel ou prestação.")
            BotaoLinha("Ver despesas mensais", acoes.contas, icone = R.drawable.ic_conta_mes)
        }

        Cartao {
            TituloCartao("Cópia de segurança")
            val quando = ultimaCopia?.atZone(ZoneId.systemDefault())
            Text(
                if (quando == null) "Nenhuma cópia feita ainda."
                else "Última cópia: ${DataTexto.completa(quando.toLocalDate())} às ${"%02d:%02d".format(quando.hour, quando.minute)}.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (LembreteCopia.precisa(ultimaCopia, Instant.now())) Text("É hora de fazer uma cópia.", style = MaterialTheme.typography.titleMedium)
            TextoApagado("Os dados ficam só neste celular. A cópia é um arquivo .fretes que você guarda onde quiser: Google Drive, WhatsApp ou uma pasta.")
            Botao("Fazer cópia", { pedirSenha = true }, ligado = !ocupado)
            BotaoLinha("Restaurar cópia", { abrir.launch(arrayOf("*/*")) }, ligado = !ocupado)
            if (temCopiaAnterior) BotaoLinha("Voltar aos dados de antes da restauração", { confirmarVolta = true }, ligado = !ocupado)
        }
    }

    if (pedirSenha) {
        var repetir by remember { mutableStateOf("") }
        var erroSenha by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { pedirSenha = false; senha = "" },
            title = { Text("Senha da cópia (opcional)", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sem senha, quem abrir o arquivo consegue ler os valores. Com senha, guarde-a bem: se esquecer, a cópia não abre.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(senha, { senha = it; erroSenha = null }, label = { Text("Senha") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    if (senha.isNotEmpty()) {
                        OutlinedTextField(repetir, { repetir = it; erroSenha = null }, label = { Text("Repita a senha") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    }
                    Erro(erroSenha)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (senha.isNotEmpty() && senha != repetir) {
                        erroSenha = "As duas senhas não são iguais."
                    } else {
                        pedirSenha = false
                        criar.launch(Copia.nomeArquivo(hoje))
                    }
                }) { Text(if (senha.isEmpty()) "Continuar sem senha" else "Continuar", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { pedirSenha = false; senha = "" }) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
        )
    }

    if (confirmarVolta) {
        AlertDialog(
            onDismissRequest = { confirmarVolta = false },
            title = { Text("Voltar aos dados anteriores?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Os dados de agora serão trocados pelos que existiam antes da última restauração.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { confirmarVolta = false; acoes.voltarCopiaAnterior() }) { Text("Voltar", style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = { confirmarVolta = false }) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}
