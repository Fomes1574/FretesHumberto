package io.github.fomes1574.freteshumberto.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.Formato
import io.github.fomes1574.freteshumberto.core.Metas
import io.github.fomes1574.freteshumberto.core.textoMeta
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import io.github.fomes1574.freteshumberto.ui.theme.TemaMeusFretes
import java.time.LocalDate

/** As quatro abas fixas (DECISOES.md). Contas do mês abre por Histórico/Ajustes, sem quinta aba. */
enum class Aba(@StringRes val titulo: Int, @DrawableRes val icone: Int) {
    HOJE(R.string.aba_hoje, R.drawable.ic_aba_hoje),
    HISTORICO(R.string.aba_historico, R.drawable.ic_aba_historico),
    RELATORIOS(R.string.aba_relatorios, R.drawable.ic_aba_relatorios),
    AJUSTES(R.string.aba_ajustes, R.drawable.ic_aba_ajustes),
}

@Composable
fun FretesApp() {
    var aba by rememberSaveable { mutableStateOf(Aba.HOJE) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Cores.Cartao) {
                Aba.entries.forEach { a ->
                    NavigationBarItem(
                        selected = aba == a,
                        onClick = { aba = a },
                        // Ícone sempre acompanhado de texto: a cor nunca é a única informação.
                        icon = { Icon(painterResource(a.icone), contentDescription = null) },
                        label = { Text(stringResource(a.titulo), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cores.Verde,
                            selectedTextColor = Cores.Verde,
                            indicatorColor = Cores.VerdeClaro,
                            unselectedIconColor = Cores.Apagado,
                            unselectedTextColor = Cores.Apagado,
                        ),
                    )
                }
            }
        },
    ) { margens ->
        when (aba) {
            Aba.HOJE -> TelaHojeInicial(margens)
            Aba.HISTORICO -> TelaEmConstrucao(margens, "Histórico", "Dias agrupados por mês, comparar dois dias e Contas do mês. Etapa 3 do PLANO_ANDROID.md.")
            Aba.RELATORIOS -> TelaEmConstrucao(margens, "Relatórios", "Dia, semana, mês e período personalizado, gráfico por dia, metas e combustível. Etapa 3.")
            Aba.AJUSTES -> TelaEmConstrucao(margens, "Ajustes", "Metas, consumo, preço do litro, tamanho da letra e cópia de segurança. Etapas 3 e 5.")
        }
    }
}

/**
 * Esqueleto da tela Hoje. Já usa o módulo :core para o texto da meta, provando a ligação
 * entre os módulos. Na etapa 2/3 o lucro vem do banco via ViewModel.
 */
@Composable
private fun TelaHojeInicial(margens: PaddingValues) {
    val hoje = LocalDate.now()
    val lucro = 0L
    val meta = Metas().doDia(hoje)
    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Cartao {
            Text("LUCRO DE HOJE", style = MaterialTheme.typography.labelMedium, color = Cores.Apagado)
            Text(Formato.reais(lucro), style = MaterialTheme.typography.displayLarge)
            Text(textoMeta(lucro, meta), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("A estrada com a Fiorino entra aqui (ui/estrada, regras em core/Estrada.kt).", style = MaterialTheme.typography.bodyMedium, color = Cores.Apagado)
        }
    }
}

@Composable
private fun TelaEmConstrucao(margens: PaddingValues, titulo: String, texto: String) {
    Column(Modifier.fillMaxSize().padding(margens).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Cartao { Text(texto, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
fun Cartao(conteudo: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Cores.Cartao),
    ) {
        Column(Modifier.padding(18.dp)) { conteudo() }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviaApp() {
    TemaMeusFretes { FretesApp() }
}
