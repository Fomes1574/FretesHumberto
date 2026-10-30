package io.github.fomes1574.freteshumberto.ui

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fomes1574.freteshumberto.R
import io.github.fomes1574.freteshumberto.core.DataTexto
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.FiltroRelatorio
import io.github.fomes1574.freteshumberto.core.LembreteCopia
import io.github.fomes1574.freteshumberto.core.ModoRelatorio
import io.github.fomes1574.freteshumberto.core.ContasDoMes
import io.github.fomes1574.freteshumberto.ui.ajustes.AcoesAjustes
import io.github.fomes1574.freteshumberto.ui.ajustes.TelaAjustes
import io.github.fomes1574.freteshumberto.ui.comum.Cartao
import io.github.fomes1574.freteshumberto.ui.comum.Erro
import io.github.fomes1574.freteshumberto.ui.contas.AcoesContas
import io.github.fomes1574.freteshumberto.ui.contas.TelaContas
import io.github.fomes1574.freteshumberto.ui.contas.TelaFormConta
import io.github.fomes1574.freteshumberto.ui.historico.TelaHistorico
import io.github.fomes1574.freteshumberto.ui.hoje.AcoesHoje
import io.github.fomes1574.freteshumberto.ui.hoje.TelaHoje
import io.github.fomes1574.freteshumberto.ui.lancamento.Formulario
import io.github.fomes1574.freteshumberto.ui.lancamento.TelaCalculadora
import io.github.fomes1574.freteshumberto.ui.lancamento.TelaLancamento
import io.github.fomes1574.freteshumberto.ui.relatorios.AcoesRelatorio
import io.github.fomes1574.freteshumberto.ui.relatorios.TelaDoisDias
import io.github.fomes1574.freteshumberto.ui.relatorios.TelaEscolherPeriodo
import io.github.fomes1574.freteshumberto.ui.relatorios.TelaRelatorios
import io.github.fomes1574.freteshumberto.ui.theme.Cores
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

/** As quatro abas fixas (DECISOES.md). Contas do mês abre por Histórico/Ajustes, sem quinta aba. */
enum class Aba(@param:StringRes val titulo: Int, @param:DrawableRes val icone: Int) {
    HOJE(R.string.aba_hoje, R.drawable.ic_aba_hoje),
    HISTORICO(R.string.aba_historico, R.drawable.ic_aba_historico),
    RELATORIOS(R.string.aba_relatorios, R.drawable.ic_aba_relatorios),
    AJUSTES(R.string.aba_ajustes, R.drawable.ic_aba_ajustes),
}

@Composable
fun FretesApp(vm: FretesViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val erroLeitura by vm.erroLeitura.collectAsStateWithLifecycle()
    val hoje by vm.hoje.collectAsStateWithLifecycle()
    val aba by vm.aba.collectAsStateWithLifecycle()
    val pilha by vm.pilha.collectAsStateWithLifecycle()
    val ocupado by vm.ocupado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.aoVoltar() }

    LaunchedEffect(Unit) {
        vm.avisos.collect { a ->
            avisos.currentSnackbarData?.dismiss()
            if (a.acao != null) {
                // "Desfazer" fica 6 segundos.
                val r = withTimeoutOrNull(6_000) { avisos.showSnackbar(a.texto, a.acao, duration = SnackbarDuration.Indefinite) }
                if (r == SnackbarResult.ActionPerformed) a.aoAgir?.invoke()
            } else {
                avisos.showSnackbar(a.texto, duration = SnackbarDuration.Short)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = Cores.Cartao) {
                    Aba.entries.forEach { a ->
                        NavigationBarItem(
                            selected = aba == a,
                            onClick = { vm.aba.value = a },
                            // Ícone sempre acompanhado de texto: a cor nunca é a única informação.
                            icon = { Icon(painterResource(a.icone), contentDescription = null) },
                            label = { Text(stringResource(a.titulo), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
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
            val e = estado
            when {
                erroLeitura != null -> AvisoErro(margens, erroLeitura!!)
                e == null -> Box(Modifier.fillMaxSize().padding(margens), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cores.Verde) }
                else -> Abas(vm, e, aba, hoje, margens)
            }
        }

        val e = estado
        val topo = pilha.lastOrNull()
        if (e != null && topo != null) Sobrepostas(vm, e, topo, hoje, ocupado)

        SnackbarHost(
            avisos,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = if (topo == null) 84.dp else 8.dp, start = 12.dp, end = 12.dp),
        ) { dados ->
            Snackbar(
                dados,
                containerColor = Cores.Tinta,
                contentColor = androidx.compose.ui.graphics.Color.White,
                actionColor = Cores.VerdeClaro,
            )
        }
    }

    DialogosRestauracao(vm)
}

@Composable
private fun AvisoErro(margens: PaddingValues, texto: String) {
    Column(Modifier.fillMaxSize().padding(margens).padding(16.dp)) {
        Cartao { Erro(texto) }
    }
}

@Composable
private fun Abas(vm: FretesViewModel, e: Estado, aba: Aba, hoje: java.time.LocalDate, margens: PaddingValues) {
    val contexto = LocalContext.current
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val letra by vm.letra.collectAsStateWithLifecycle()
    val ultimaCopia by vm.ultimaCopia.collectAsStateWithLifecycle()
    val temCopiaAnterior by vm.temCopiaAnterior.collectAsStateWithLifecycle()
    val ocupado by vm.ocupado.collectAsStateWithLifecycle()

    val novo = { t: io.github.fomes1574.freteshumberto.core.Tipo, d: java.time.LocalDate -> vm.abrir(Sobreposta.FormLancamento(Formulario(t, null, d))) }
    val editar = { l: io.github.fomes1574.freteshumberto.core.Lancamento -> vm.abrir(Sobreposta.FormLancamento(Formulario(l.tipo, l, l.data))) }

    when (aba) {
        Aba.HOJE -> TelaHoje(
            e, hoje, vm.posicoesVan, LembreteCopia.precisa(ultimaCopia, Instant.now()), margens,
            AcoesHoje(
                novo = novo,
                calcular = { vm.abrir(Sobreposta.Calculadora) },
                editar = editar,
                apagar = vm::apagar,
                conferir = vm::conferir,
                verMes = {
                    vm.filtro.value = FiltroRelatorio(ModoRelatorio.MES, hoje)
                    vm.aba.value = Aba.RELATORIOS
                },
                irCopia = { vm.aba.value = Aba.AJUSTES },
            ),
        )
        Aba.HISTORICO -> TelaHistorico(
            e, hoje, margens,
            aoAbrirDia = vm::abrirDia,
            aoCompararDias = { vm.abrir(Sobreposta.DoisDias) },
            aoContas = { vm.abrir(Sobreposta.Contas) },
        )
        Aba.RELATORIOS -> TelaRelatorios(
            e, filtro, hoje, vm.posicoesVan, margens,
            AcoesRelatorio(
                mudarFiltro = { vm.filtro.value = it },
                escolherPeriodo = { vm.abrir(Sobreposta.Periodo) },
                escolherDoisDias = { vm.abrir(Sobreposta.DoisDias) },
                novo = novo,
                editar = editar,
                apagar = vm::apagar,
                conferir = vm::conferir,
                compartilhar = { texto ->
                    val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
                    contexto.startActivity(Intent.createChooser(envio, "Mandar resumo"))
                },
            ),
        )
        Aba.AJUSTES -> TelaAjustes(
            e, hoje, letra, ultimaCopia, temCopiaAnterior, ocupado, margens,
            AcoesAjustes(
                salvar = vm::salvarAjustes,
                mudarLetra = vm::mudarLetra,
                contas = { vm.abrir(Sobreposta.Contas) },
                fazerCopia = vm::fazerCopia,
                restaurar = vm::abrirCopia,
                voltarCopiaAnterior = vm::voltarCopiaAnterior,
            ),
        )
    }
}

@Composable
private fun Sobrepostas(vm: FretesViewModel, e: Estado, topo: Sobreposta, hoje: java.time.LocalDate, ocupado: Boolean) {
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val mesContas by vm.mesContas.collectAsStateWithLifecycle()
    when (topo) {
        is Sobreposta.FormLancamento -> TelaLancamento(
            topo.formulario, hoje, e.precoReferencia(), ocupado, vm::novoId, vm::agora,
            aoSalvar = { l, outro, limpar ->
                vm.salvar(l, topo.formulario.existente != null, outro) { if (outro) limpar() else vm.fecharTopo() }
            },
            aoFechar = vm::fecharTopo,
        )
        Sobreposta.Calculadora -> TelaCalculadora(e.ajustes.kmPorLitroCentesimos, e.precoReferencia(), vm::fecharTopo)
        Sobreposta.Periodo -> TelaEscolherPeriodo(filtro, hoje, { vm.filtro.value = it; vm.aba.value = Aba.RELATORIOS; vm.fecharTopo() }, vm::fecharTopo)
        Sobreposta.DoisDias -> TelaDoisDias(filtro, hoje, { vm.filtro.value = it; vm.aba.value = Aba.RELATORIOS; vm.fecharTopo() }, vm::fecharTopo)
        Sobreposta.Contas -> TelaContas(
            e, mesContas, ocupado,
            AcoesContas(
                mudarMes = { vm.mesContas.value = it },
                nova = { vm.abrir(Sobreposta.FormConta(null)) },
                editar = { vm.abrir(Sobreposta.FormConta(it.conta.id)) },
                pular = { vm.alternarPulo(it.conta.id, mesContas) },
                pausar = { vm.alternarPausa(it.conta.id, mesContas) },
                verLancamento = { id -> e.lancamentos.firstOrNull { it.id == id }?.let { l -> vm.abrir(Sobreposta.FormLancamento(Formulario(l.tipo, l, l.data))) } },
                fechar = vm::fecharTopo,
            ),
        )
        is Sobreposta.FormConta -> {
            val situacao = topo.contaId?.let { id -> ContasDoMes.situacoes(e, mesContas).firstOrNull { it.conta.id == id } }
            TelaFormConta(
                situacao, mesContas, hoje, ocupado,
                aoCadastrar = { nome, valor, dia, inicio, fim -> vm.cadastrarConta(nome, valor, dia, inicio, fim, vm::fecharTopo) },
                aoEditar = { so, nome, valor, dia, fim -> vm.editarConta(topo.contaId!!, mesContas, so, nome, valor, dia, fim, vm::fecharTopo) },
                aoFechar = vm::fecharTopo,
            )
        }
    }
}

/** Passos de "Restaurar cópia": senha, confirmação e erros. Nada muda antes de confirmar. */
@Composable
private fun DialogosRestauracao(vm: FretesViewModel) {
    val r by vm.restauracao.collectAsStateWithLifecycle()
    when (val passo = r) {
        null -> Unit
        Restauracao.Lendo -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Lendo a cópia…", style = MaterialTheme.typography.titleLarge) },
            text = { CircularProgressIndicator(color = Cores.Verde) },
        )
        is Restauracao.Erro -> AlertDialog(
            onDismissRequest = vm::cancelarRestauracao,
            title = { Text("Não deu para restaurar", style = MaterialTheme.typography.titleLarge) },
            text = { Text(passo.mensagem + " Seus dados atuais não foram alterados.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = vm::cancelarRestauracao) { Text("Entendi", style = MaterialTheme.typography.labelLarge) } },
        )
        is Restauracao.PedirSenha -> {
            var senha by remember(passo) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = vm::cancelarRestauracao,
                title = { Text("Senha da cópia", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Esta cópia foi feita com senha.", style = MaterialTheme.typography.bodyLarge)
                        OutlinedTextField(
                            senha, { senha = it }, label = { Text("Senha") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        Erro(passo.erro)
                    }
                },
                confirmButton = { TextButton(onClick = { vm.informarSenha(senha) }, enabled = senha.isNotEmpty()) { Text("Abrir", style = MaterialTheme.typography.labelLarge) } },
                dismissButton = { TextButton(onClick = vm::cancelarRestauracao) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
            )
        }
        is Restauracao.Confirmar -> {
            val l = passo.novo.lancamentos
            val periodo = if (l.isEmpty()) "sem lançamentos" else "${l.size} lançamentos, de ${DataTexto.completa(l.minOf { it.data })} a ${DataTexto.completa(l.maxOf { it.data })}"
            AlertDialog(
                onDismissRequest = vm::cancelarRestauracao,
                title = { Text("Restaurar esta cópia?", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Text(
                        "A cópia tem $periodo e ${passo.novo.contas.size} contas do mês.\n\n" +
                            "Restaurar troca todos os dados deste celular pelos da cópia. Os dados de agora ficam guardados no app (sem senha) para você poder voltar.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                confirmButton = { TextButton(onClick = vm::confirmarRestauracao) { Text("Restaurar", style = MaterialTheme.typography.labelLarge) } },
                dismissButton = { TextButton(onClick = vm::cancelarRestauracao) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
            )
        }
    }
}
