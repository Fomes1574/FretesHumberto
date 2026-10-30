@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.fomes1574.freteshumberto.ui.comum

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Cartão branco de cantos arredondados, base de todas as telas (ARQUITETURA §4). */
@Composable
fun Cartao(modifier: Modifier = Modifier, cor: Color = Cores.Cartao, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cor, contentColor = Cores.Tinta),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = conteudo)
    }
}

@Composable
fun TituloTela(texto: String, subtitulo: String? = null) {
    Column {
        Text(texto, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyLarge, color = Cores.Apagado)
    }
}

@Composable
fun TituloCartao(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
fun Rotulo(texto: String) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelMedium, color = Cores.Apagado)
}

@Composable
fun TextoApagado(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado, modifier = modifier)
}

/** Valor em reais; negativo em vermelho e sempre com sinal (R02). */
@Composable
fun Valor(centavos: Long, modifier: Modifier = Modifier, grande: Boolean = false) {
    Text(
        Formato.reais(centavos),
        style = if (grande) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleMedium,
        color = if (centavos < 0) Cores.Negativo else Cores.Tinta,
        modifier = modifier,
    )
}

@Composable
fun Botao(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icone: Int? = null,
    fundo: Color = Cores.Verde,
    cor: Color = Color.White,
    ligado: Boolean = true,
    alto: Boolean = false,
) {
    Button(
        onClick = aoTocar,
        enabled = ligado,
        modifier = modifier.fillMaxWidth().heightIn(min = if (alto) 64.dp else 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = fundo, contentColor = cor),
    ) {
        ConteudoBotao(texto, icone)
    }
}

@Composable
fun BotaoLinha(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icone: Int? = null,
    ligado: Boolean = true,
    cor: Color = Cores.Tinta,
) {
    OutlinedButton(
        onClick = aoTocar,
        enabled = ligado,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, Cores.Linha),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Cores.Cartao, contentColor = cor),
    ) {
        ConteudoBotao(texto, icone)
    }
}

@Composable
private fun RowScope.ConteudoBotao(texto: String, @DrawableRes icone: Int?) {
    if (icone != null) {
        Icon(painterResource(icone), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
    }
    Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Botão só com ícone, sempre com descrição para o leitor de tela. Área de toque 48 dp. */
@Composable
fun BotaoIcone(@DrawableRes icone: Int, descricao: String, aoTocar: () -> Unit, ligado: Boolean = true) {
    IconButton(onClick = aoTocar, enabled = ligado, modifier = Modifier.size(52.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = Cores.Fundo) {
            Icon(painterResource(icone), contentDescription = descricao, modifier = Modifier.padding(10.dp).size(24.dp))
        }
    }
}

/** Linha "rótulo .... valor" das contas (Conta de hoje, Conta do período). */
@Composable
fun LinhaConta(rotulo: String, valor: String, detalhe: String? = null, total: Boolean = false, negativo: Boolean = false) {
    if (total) HorizontalDivider(thickness = 2.dp, color = Cores.Tinta)
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(rotulo, style = if (total) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
            if (detalhe != null) {
                Spacer(Modifier.width(6.dp))
                Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado)
            }
        }
        Text(
            valor,
            style = if (total) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (negativo) Cores.Negativo else Cores.Tinta,
        )
    }
    if (!total) HorizontalDivider(color = Cores.Linha)
}

/** Número grande com legenda (blocos de métricas). */
@Composable
fun Kpi(numero: String, legenda: String, modifier: Modifier = Modifier, fundo: Color = Cores.Fundo, negativo: Boolean = false) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = fundo) {
        Column(Modifier.padding(14.dp)) {
            Text(numero, style = MaterialTheme.typography.titleLarge, color = if (negativo) Cores.Negativo else Cores.Tinta)
            Text(legenda, style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado)
        }
    }
}

/** Dois blocos lado a lado; com letra grande eles continuam legíveis porque quebram linha. */
@Composable
fun Par(esquerda: @Composable (Modifier) -> Unit, direita: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        esquerda(Modifier.weight(1f))
        direita(Modifier.weight(1f))
    }
}

@Composable
fun Selo(texto: String, fundo: Color = Cores.VerdeClaro, cor: Color = Cores.Verde) {
    Surface(shape = RoundedCornerShape(8.dp), color = fundo) {
        Text(texto, style = MaterialTheme.typography.labelSmall, color = cor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/**
 * Campo de número com teclado decimal. Valores colados com milhar são convertidos sem mudar o
 * valor; no máximo duas casas (R03).
 */
@Composable
fun CampoNumero(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefixo: String? = "R$",
    grande: Boolean = false,
    inteiro: Boolean = false,
    dica: String? = null,
    ultimo: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(rotulo, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = valor,
            onValueChange = { novo -> aoMudar(if (inteiro) novo.filter { it.isDigit() }.take(6) else io.github.fomes1574.freteshumberto.core.Dinheiro.campo(novo)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = if (grande) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
            prefix = prefixo?.let { { Text("$it ", style = MaterialTheme.typography.titleMedium, color = Cores.Apagado) } },
            placeholder = dica?.let { { Text(it, color = Cores.Apagado) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (inteiro) KeyboardType.Number else KeyboardType.Decimal,
                imeAction = if (ultimo) ImeAction.Done else ImeAction.Next,
            ),
            shape = RoundedCornerShape(14.dp),
            colors = coresCampo(),
        )
    }
}

@Composable
fun CampoTexto(rotulo: String, valor: String, aoMudar: (String) -> Unit, opcional: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rotulo, style = MaterialTheme.typography.titleMedium)
            if (opcional) Text(" (opcional)", style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado)
        }
        OutlinedTextField(
            value = valor,
            onValueChange = { aoMudar(it.take(160)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            colors = coresCampo(),
        )
    }
}

@Composable
private fun coresCampo() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Cores.Cartao,
    unfocusedContainerColor = Cores.Cartao,
    focusedBorderColor = Cores.Verde,
    unfocusedBorderColor = Cores.Linha,
)

private fun LocalDate.millisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.dataUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Data com atalhos "Hoje" e "Ontem" e o calendário para outras datas. [maximo] impede
 * data futura nos lançamentos (R03).
 */
@Composable
fun CampoData(rotulo: String, data: LocalDate?, aoMudar: (LocalDate?) -> Unit, hoje: LocalDate, maximo: LocalDate? = hoje, opcional: Boolean = false) {
    var aberto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(rotulo, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!opcional) {
                Escolha("Hoje", data == hoje, { aoMudar(hoje) }, Modifier.weight(1f))
                Escolha("Ontem", data == hoje.minusDays(1), { aoMudar(hoje.minusDays(1)) }, Modifier.weight(1f))
            }
        }
        OutlinedButton(
            onClick = { aberto = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, Cores.Linha),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Cores.Cartao, contentColor = Cores.Tinta),
        ) {
            Icon(painterResource(R.drawable.ic_calendario), contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                data?.let { "${DataTexto.longa(it)} de ${it.year}" } ?: "Escolher data",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (opcional && data != null) TextButton(onClick = { aoMudar(null) }) { Text("Tirar a data", style = MaterialTheme.typography.labelLarge) }
    }
    if (aberto) {
        val limite = maximo?.millisUtc()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (data ?: hoje).millisUtc(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = limite == null || utcTimeMillis <= limite
            },
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { aoMudar(it.dataUtc()) }
                    aberto = false
                }) { Text("Usar esta data", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
        ) {
            DatePicker(state = estado, showModeToggle = false)
        }
    }
}

/** Botão de escolha (chip grande). O estado escolhido é anunciado ao leitor de tela. */
@Composable
fun Escolha(texto: String, escolhido: Boolean, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val m = modifier.heightIn(min = 52.dp).semantics { selected = escolhido; role = Role.RadioButton }
    if (escolhido) {
        Button(onClick = aoTocar, modifier = m, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Cores.Verde)) {
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(
            onClick = aoTocar, modifier = m, shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, Cores.Linha),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Cores.Cartao, contentColor = Cores.Tinta),
        ) {
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Seletor segmentado (Dia/Semana/Mês/Período, preço/litros, A/A+/A++). */
@Composable
fun Segmentado(opcoes: List<String>, escolhido: Int, aoEscolher: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE7E3D8), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            opcoes.forEachIndexed { i, t ->
                val sel = i == escolhido
                TextButton(
                    onClick = { aoEscolher(i) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = sel; role = Role.Tab },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (sel) Cores.Cartao else Color.Transparent,
                        contentColor = Cores.Tinta,
                    ),
                ) {
                    Text(t, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Mensagem de erro do formulário, lida pelo leitor de tela. */
@Composable
fun Erro(texto: String?) {
    if (texto != null) Text(texto, style = MaterialTheme.typography.titleMedium, color = Cores.Negativo)
}

/**
 * Tela inteira por cima das abas (formulários, calculadora, contas do mês). Mais estável que
 * folha deslizante com teclado aberto e letra grande. Voltar do Android fecha.
 */
@Composable
fun TelaSobreposta(titulo: String, aoFechar: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = aoFechar)
    Box(Modifier.fillMaxSize().background(Cores.Fundo)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titulo, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
                BotaoIcone(R.drawable.ic_fechar, "Fechar", aoFechar)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                conteudo()
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}
