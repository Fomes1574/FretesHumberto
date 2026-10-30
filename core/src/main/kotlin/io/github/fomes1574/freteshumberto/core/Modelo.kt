package io.github.fomes1574.freteshumberto.core

import java.time.LocalDate
import java.time.YearMonth

enum class Tipo { FRETE, ABASTECIMENTO, DESPESA }

enum class ModoAbastecimento { PRECO, LITROS }

/** Categorias de "Outra despesa". "Outros" é o padrão. */
val CATEGORIAS = listOf("Pedágio", "Manutenção", "Estacionamento", "Lavagem", "Multa", "Outros")

/**
 * Um lançamento do diário. Os três tipos são independentes (R01): nenhum frete
 * tem vínculo com despesa ou abastecimento.
 *
 * Unidades: [valor] e [precoLitro] em centavos; [kmCentesimos] e [litrosCentesimos]
 * em centésimos; [minutos] inteiro.
 */
data class Lancamento(
    val id: String,
    val tipo: Tipo,
    val data: LocalDate,
    val valor: Long,
    val descricao: String = "",
    // frete (opcionais, só referência)
    val kmCentesimos: Long? = null,
    val minutos: Int? = null,
    // abastecimento
    val litrosCentesimos: Long? = null,
    val precoLitro: Long? = null,
    val modoAbastecimento: ModoAbastecimento? = null,
    // despesa
    val categoria: String? = null,
    /** Chave "recId|AAAA-MM" quando gerada por uma conta do mês. */
    val ocorrencia: String? = null,
    /** Instantes técnicos (ISO-8601), separados da data financeira. */
    val criadoEm: String = "",
    val editadoEm: String? = null,
) {
    init {
        require(valor in 1..Dinheiro.MAX) { "Valor deve ser positivo" }
        require(descricao.length <= 1000) { "Descrição longa demais" }
        require(minutos == null || minutos >= 0) { "Duração inválida" }
        require(kmCentesimos == null || kmCentesimos >= 0) { "Distância inválida" }
        if (tipo == Tipo.ABASTECIMENTO) {
            require((litrosCentesimos ?: 0) > 0 && (precoLitro ?: 0) > 0) { "Abastecimento precisa de litros e preço" }
        }
    }
}

/** Meta diária com vigência: vale de [desde] em diante até a próxima. */
data class MetaDiaria(val desde: LocalDate, val valor: Long)

/**
 * Metas (R06/R07 do protótipo). Mudar a meta não reescreve dias anteriores;
 * a meta mensal pertence ao mês escolhido.
 */
data class Metas(
    val diarias: List<MetaDiaria> = listOf(MetaDiaria(LocalDate.of(1900, 1, 1), 25_000)),
    val mensais: Map<YearMonth, Long> = emptyMap(),
    val baseMensal: Long = 550_000,
) {
    private val ordenadas = diarias.sortedBy { it.desde }

    fun doDia(d: LocalDate): Long {
        var v = ordenadas.firstOrNull()?.valor ?: 0
        for (g in ordenadas) if (!g.desde.isAfter(d)) v = g.valor
        return v
    }

    fun doMes(m: YearMonth): Long = mensais[m] ?: baseMensal

    /** Nova meta diária a partir de [desde]; substitui só uma meta com a mesma data de início. */
    fun comMetaDiaria(desde: LocalDate, valor: Long): Metas =
        copy(diarias = (diarias.filter { it.desde != desde } + MetaDiaria(desde, valor)).sortedBy { it.desde })

    fun comMetaMensal(m: YearMonth, valor: Long): Metas = copy(mensais = mensais + (m to valor))
}

/** Texto da meta, sem julgamento (R06): "Faltam R$ X", "Meta atingida", "Meta atingida · R$ X acima". */
fun textoMeta(lucro: Long, meta: Long, sufixo: String = "para a meta"): String {
    val dif = lucro - meta
    return when {
        dif < 0 -> "Faltam ${Formato.reais(-dif)} $sufixo"
        dif == 0L -> "Meta atingida"
        else -> "Meta atingida · ${Formato.reais(dif)} acima"
    }
}
