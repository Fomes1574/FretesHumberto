package io.github.fomes1574.freteshumberto.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fomes1574.freteshumberto.FretesApplication
import io.github.fomes1574.freteshumberto.backup.ArquivosCopia
import io.github.fomes1574.freteshumberto.core.Copia
import io.github.fomes1574.freteshumberto.core.CopiaInvalida
import io.github.fomes1574.freteshumberto.core.Diario
import io.github.fomes1574.freteshumberto.core.Estado
import io.github.fomes1574.freteshumberto.core.FiltroRelatorio
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.ModoRelatorio
import io.github.fomes1574.freteshumberto.data.TamanhoLetra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Mensagem curta no rodapé. Com [acao], mostra um botão (ex.: "Desfazer" por 6 s). */
data class Aviso(val texto: String, val acao: String? = null, val aoAgir: (() -> Unit)? = null)

/** Passos de "Restaurar cópia". Nada é trocado antes da confirmação. */
sealed interface Restauracao {
    data object Lendo : Restauracao
    data class PedirSenha(val arquivo: String, val erro: String? = null) : Restauracao
    data class Confirmar(val novo: Estado) : Restauracao
    data class Erro(val mensagem: String) : Restauracao
}

/** Telas que abrem por cima das abas. Ficam numa pilha: fechar volta à anterior. */
sealed interface Sobreposta {
    data class FormLancamento(val formulario: io.github.fomes1574.freteshumberto.ui.lancamento.Formulario) : Sobreposta
    data object Calculadora : Sobreposta
    data object Contas : Sobreposta
    data class FormConta(val contaId: String?) : Sobreposta
    data object Periodo : Sobreposta
    data object DoisDias : Sobreposta
}

private const val FALHA_GRAVAR = "Não foi possível salvar. Seus dados anteriores foram mantidos."

/**
 * Estado das telas e ações. As regras ficam no :core e a gravação no Repositorio; aqui só
 * se chama a operação, se protege contra toque duplo e se avisa o resultado verdadeiro.
 */
class FretesViewModel(private val fretes: FretesApplication) : ViewModel() {
    private val repo = fretes.repositorio
    private val prefs = fretes.preferencias
    private val arquivos = ArquivosCopia(fretes)

    private val _erroLeitura = MutableStateFlow<String?>(null)
    /** Falha ao ler o banco: a tela mostra o erro e não troca dados por exemplos (R09). */
    val erroLeitura: StateFlow<String?> = _erroLeitura

    /** Null enquanto carrega. */
    val estado: StateFlow<Estado?> = repo.estado
        .catch { _erroLeitura.value = "Não foi possível ler os dados do app. Feche e abra de novo. Nada foi apagado." }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val letra: StateFlow<TamanhoLetra> = prefs.letra.stateIn(viewModelScope, SharingStarted.Eagerly, TamanhoLetra.NORMAL)
    val ultimaCopia: StateFlow<Instant?> = prefs.ultimaCopia.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _hoje = MutableStateFlow(repo.hoje())
    val hoje: StateFlow<LocalDate> = _hoje

    val filtro = MutableStateFlow(FiltroRelatorio(ModoRelatorio.SEMANA, repo.hoje()))

    val aba = MutableStateFlow(Aba.HOJE)

    private val _pilha = MutableStateFlow<List<Sobreposta>>(emptyList())
    val pilha: StateFlow<List<Sobreposta>> = _pilha

    /** Mês mostrado em Contas do mês. */
    val mesContas = MutableStateFlow(YearMonth.from(repo.hoje()))

    fun abrir(t: Sobreposta) {
        _pilha.value = _pilha.value + t
    }

    fun fecharTopo() {
        _pilha.value = _pilha.value.dropLast(1)
    }

    private val _ocupado = MutableStateFlow(false)
    /** Enquanto grava, os botões de salvar ficam desligados: dois toques = um lançamento. */
    val ocupado: StateFlow<Boolean> = _ocupado

    private val _avisos = MutableSharedFlow<Aviso>(extraBufferCapacity = 8)
    val avisos: SharedFlow<Aviso> = _avisos

    private val _restauracao = MutableStateFlow<Restauracao?>(null)
    val restauracao: StateFlow<Restauracao?> = _restauracao

    private val _temCopiaAnterior = MutableStateFlow(arquivos.temCopiaPreventiva())
    /** Existe a cópia automática feita antes da última restauração. */
    val temCopiaAnterior: StateFlow<Boolean> = _temCopiaAnterior

    /** Última posição da van em cada cena: voltar à aba sem mudança não anima de novo. */
    val posicoesVan = HashMap<String, Float>()

    init {
        // Virada do dia às 00:00 de Brasília com o app aberto: troca "hoje" (dia novo, sem
        // fretes nem despesas) e lança as contas que venceram. Confere no máximo a cada minuto,
        // para acertar mesmo se o celular dormir ou o relógio mudar.
        viewModelScope.launch {
            while (true) {
                delay(minOf(repo.ateMeiaNoite().toMillis() + 500, 60_000))
                if (repo.hoje() != _hoje.value) aoVoltar()
            }
        }
    }

    /** Ao abrir ou voltar ao app: atualiza a data e gera as contas do mês devidas (R08). */
    fun aoVoltar() {
        val h = repo.hoje()
        if (h != _hoje.value) {
            val f = filtro.value
            if (f.referencia == _hoje.value) filtro.value = f.copy(referencia = h)
            _hoje.value = h
        }
        viewModelScope.launch { runCatching { repo.gerarContas(h) } }
    }

    fun avisar(texto: String) {
        _avisos.tryEmit(Aviso(texto))
    }

    /** Roda uma gravação. Só chama [depois] e mostra [sucesso] se a gravação deu certo. */
    private fun gravar(sucesso: String?, depois: () -> Unit = {}, bloco: suspend () -> Unit) {
        if (_ocupado.value) return
        _ocupado.value = true
        viewModelScope.launch {
            try {
                bloco()
                depois()
                sucesso?.let { avisar(it) }
            } catch (e: Exception) {
                avisar(FALHA_GRAVAR)
            } finally {
                _ocupado.value = false
            }
        }
    }

    fun novoId(): String = repo.novoId()

    fun agora(): String = repo.agora()

    fun salvar(l: Lancamento, editando: Boolean, outro: Boolean, depois: () -> Unit) =
        gravar(if (outro) "Frete salvo. Pode anotar o próximo." else if (editando) "Corrigido." else "Salvo.", depois) { repo.salvar(l) }

    /** Apaga só este lançamento e oferece "Desfazer" por 6 segundos. */
    fun apagar(l: Lancamento) {
        viewModelScope.launch {
            val r: Diario.Removido? = try {
                repo.apagar(l.id)
            } catch (e: Exception) {
                avisar("Não foi possível apagar. Nada foi alterado.")
                return@launch
            }
            if (r != null) _avisos.tryEmit(Aviso("Apagado.", "Desfazer") { desfazer(r) })
        }
    }

    private fun desfazer(r: Diario.Removido) {
        viewModelScope.launch {
            try {
                repo.desfazer(r)
                avisar("Lançamento de volta.")
            } catch (e: Exception) {
                avisar(FALHA_GRAVAR)
            }
        }
    }

    fun salvarAjustes(metaDia: Long, kmPorLitro: Long, preco: Long) =
        gravar("Ajustes salvos.") { repo.salvarAjustes(metaDia, kmPorLitro, preco) }

    fun mudarLetra(t: TamanhoLetra) {
        viewModelScope.launch { prefs.mudarLetra(t) }
    }

    // ---------------------------------------------------------------- relatórios

    fun abrirDia(d: LocalDate) {
        filtro.value = FiltroRelatorio(ModoRelatorio.DIA, d)
        aba.value = Aba.RELATORIOS
    }

    // ---------------------------------------------------------------- contas do mês

    fun cadastrarConta(nome: String, valor: Long, dia: Int, inicio: LocalDate, fim: LocalDate?, depois: () -> Unit) =
        gravar("Conta mensal salva.", depois) { repo.cadastrarConta(nome, valor, dia, inicio, fim) }

    fun editarConta(id: String, mes: YearMonth, soEsteMes: Boolean, nome: String, valor: Long, dia: Int, fim: LocalDate?, depois: () -> Unit) =
        gravar("Conta mensal salva.", depois) {
            if (soEsteMes) repo.editarContaSoEsteMes(id, mes, nome, valor, dia) else repo.editarContaDesteMes(id, mes, nome, valor, dia, fim)
        }

    fun alternarPulo(id: String, mes: YearMonth) = gravar("Conta atualizada.") { repo.alternarPulo(id, mes) }

    fun alternarPausa(id: String, mes: YearMonth) = gravar("Repetição atualizada.") { repo.alternarPausa(id, mes) }

    // ---------------------------------------------------------------- cópia de segurança

    /**
     * Grava a cópia no arquivo escolhido. Só registra "última cópia" depois de o arquivo
     * ser escrito e conferido (R09).
     */
    fun fazerCopia(destino: Uri, senha: String?) = gravar(null) {
        try {
            withContext(Dispatchers.Default) {
                val json = Copia.paraJson(repo.carregar(), repo.agora())
                val conteudo = if (senha.isNullOrEmpty()) json else Copia.cifrar(json, senha)
                withContext(Dispatchers.IO) { arquivos.escrever(destino, conteudo) }
            }
            prefs.registrarCopia(Instant.now())
            avisar(if (senha.isNullOrEmpty()) "Cópia salva (sem senha)." else "Cópia salva com senha.")
        } catch (e: Exception) {
            avisar("Não foi possível salvar a cópia no arquivo escolhido. Tente outro lugar.")
        }
    }

    fun abrirCopia(origem: Uri) {
        _restauracao.value = Restauracao.Lendo
        viewModelScope.launch {
            val texto = try {
                withContext(Dispatchers.IO) { arquivos.ler(origem) }
            } catch (e: Exception) {
                _restauracao.value = Restauracao.Erro("Não foi possível ler o arquivo escolhido.")
                return@launch
            }
            if (Copia.temSenha(texto)) _restauracao.value = Restauracao.PedirSenha(texto) else validar(texto, null)
        }
    }

    fun informarSenha(senha: String) {
        val p = _restauracao.value as? Restauracao.PedirSenha ?: return
        _restauracao.value = Restauracao.Lendo
        viewModelScope.launch { validar(p.arquivo, senha) }
    }

    private suspend fun validar(texto: String, senha: String?) {
        _restauracao.value = try {
            Restauracao.Confirmar(withContext(Dispatchers.Default) { Copia.abrir(texto, senha) })
        } catch (e: CopiaInvalida) {
            if (senha != null && Copia.temSenha(texto)) Restauracao.PedirSenha(texto, e.mensagem) else Restauracao.Erro(e.mensagem)
        } catch (e: Exception) {
            Restauracao.Erro("O arquivo não é uma cópia do Meus Fretes ou está danificado.")
        }
    }

    fun cancelarRestauracao() {
        _restauracao.value = null
    }

    /**
     * Guarda os dados atuais numa cópia automática (sem senha, só dentro do app) e troca
     * tudo pela cópia escolhida, numa transação. Se falhar, nada muda.
     */
    fun confirmarRestauracao() {
        val c = _restauracao.value as? Restauracao.Confirmar ?: return
        _restauracao.value = null
        gravar(null) {
            try {
                val atual = Copia.paraJson(repo.carregar(), repo.agora())
                withContext(Dispatchers.IO) { arquivos.guardarPreventiva(atual) }
                _temCopiaAnterior.value = true
            } catch (e: Exception) {
                avisar("Não foi possível guardar os dados atuais antes de restaurar. Nada foi alterado.")
                return@gravar
            }
            try {
                repo.substituirTudo(c.novo)
                repo.gerarContas()
                avisar("Cópia restaurada.")
            } catch (e: Exception) {
                avisar("Não foi possível restaurar. Seus dados anteriores foram mantidos.")
            }
        }
    }

    /** Volta aos dados de antes da última restauração. */
    fun voltarCopiaAnterior() = gravar(null) {
        try {
            val texto = withContext(Dispatchers.IO) { arquivos.lerPreventiva() } ?: return@gravar
            repo.substituirTudo(withContext(Dispatchers.Default) { Copia.deJson(texto) })
            avisar("Dados de antes da restauração de volta.")
        } catch (e: Exception) {
            avisar("Não foi possível voltar. Os dados atuais foram mantidos.")
        }
    }
}
