package io.github.fomes1574.freteshumberto.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fomes1574.freteshumberto.core.AlteracaoMes
import io.github.fomes1574.freteshumberto.core.ContaMensal
import io.github.fomes1574.freteshumberto.core.Lancamento
import io.github.fomes1574.freteshumberto.core.ModoAbastecimento
import io.github.fomes1574.freteshumberto.core.Ocorrencia
import io.github.fomes1574.freteshumberto.core.StatusOcorrencia
import io.github.fomes1574.freteshumberto.core.Tipo
import io.github.fomes1574.freteshumberto.core.VersaoConta
import java.time.LocalDate
import java.time.YearMonth

/*
 * Esquema do banco, versão 2 (a 1 tinha meta mensal e conferência de dia). Datas financeiras ficam como texto "AAAA-MM-DD" (sem hora e sem
 * fuso) e meses como "AAAA-MM"; dinheiro em centavos (Long); litros e km em centésimos.
 *
 * Lançamento numa tabela só, com colunas opcionais por tipo: não existe coluna que ligue
 * despesa ou abastecimento a um frete (R01), então apagar ou mover um frete não tem como
 * mexer em outro lançamento.
 *
 * Alterar uma tabela exige subir a versão do banco e escrever uma migração (sem apagar dados).
 */

@Entity(
    tableName = "lancamento",
    indices = [Index("data"), Index(value = ["ocorrencia"], unique = true)],
)
data class LancamentoEntidade(
    @PrimaryKey val id: String,
    val tipo: String,
    val data: String,
    val valor: Long,
    val descricao: String,
    val kmCentesimos: Long?,
    val minutos: Int?,
    val litrosCentesimos: Long?,
    val precoLitro: Long?,
    val modoAbastecimento: String?,
    val categoria: String?,
    /** "contaId|AAAA-MM" se veio de uma conta do mês. Única: um lançamento por conta e mês. */
    val ocorrencia: String?,
    val criadoEm: String,
    val editadoEm: String?,
) {
    fun paraCore() = Lancamento(
        id = id, tipo = Tipo.valueOf(tipo), data = LocalDate.parse(data), valor = valor, descricao = descricao,
        kmCentesimos = kmCentesimos, minutos = minutos, litrosCentesimos = litrosCentesimos, precoLitro = precoLitro,
        modoAbastecimento = modoAbastecimento?.let { ModoAbastecimento.valueOf(it) }, categoria = categoria,
        ocorrencia = ocorrencia, criadoEm = criadoEm, editadoEm = editadoEm,
    )

    companion object {
        fun de(l: Lancamento) = LancamentoEntidade(
            l.id, l.tipo.name, l.data.toString(), l.valor, l.descricao, l.kmCentesimos, l.minutos, l.litrosCentesimos,
            l.precoLitro, l.modoAbastecimento?.name, l.categoria, l.ocorrencia, l.criadoEm, l.editadoEm,
        )
    }
}

@Entity(tableName = "conta")
data class ContaEntidade(@PrimaryKey val id: String, val inicio: String)

/** Versões da conta: uma por conta e mês de início de vigência. Versões antigas ficam. */
@Entity(
    tableName = "conta_versao",
    primaryKeys = ["contaId", "desde"],
    foreignKeys = [ForeignKey(entity = ContaEntidade::class, parentColumns = ["id"], childColumns = ["contaId"], onDelete = ForeignKey.CASCADE)],
)
data class VersaoContaEntidade(
    val contaId: String,
    val desde: String,
    val nome: String,
    val valor: Long,
    val dia: Int,
    val ativa: Boolean,
    val categoria: String,
    val fim: String?,
) {
    fun paraCore() = VersaoConta(YearMonth.parse(desde), nome, valor, dia, ativa, categoria, fim?.let { LocalDate.parse(it) })

    companion object {
        fun de(contaId: String, v: VersaoConta) =
            VersaoContaEntidade(contaId, v.desde.toString(), v.nome, v.valor, v.dia, v.ativa, v.categoria, v.fim?.toString())
    }
}

/**
 * Ocorrência de uma conta num mês. Chave conta+mês: o banco recusa duplicar o mesmo mês,
 * mesmo se a abertura do app e o agendador rodarem juntos. Sem chave estrangeira para o
 * lançamento: um mês pulado continua registrado depois que a despesa é apagada.
 */
@Entity(tableName = "ocorrencia", primaryKeys = ["contaId", "mes"])
data class OcorrenciaEntidade(
    val contaId: String,
    val mes: String,
    val status: String,
    val lancamentoId: String?,
    val altData: String?,
    val altValor: Long?,
    val altDescricao: String?,
    val altCategoria: String?,
) {
    fun paraCore() = Ocorrencia(
        contaId, YearMonth.parse(mes), StatusOcorrencia.valueOf(status), lancamentoId,
        if (altData != null && altValor != null) AlteracaoMes(LocalDate.parse(altData), altValor, altDescricao ?: "", altCategoria ?: "Outros") else null,
    )

    companion object {
        fun de(o: Ocorrencia) = OcorrenciaEntidade(
            o.contaId, o.mes.toString(), o.status.name, o.lancamentoId,
            o.alteracao?.data?.toString(), o.alteracao?.valor, o.alteracao?.descricao, o.alteracao?.categoria,
        )
    }
}

@Entity(tableName = "meta_diaria")
data class MetaDiariaEntidade(@PrimaryKey val desde: String, val valor: Long)

/** Ajustes simples em pares chave/valor (consumo e preço manual do litro). */
@Entity(tableName = "ajuste")
data class AjusteEntidade(@PrimaryKey val chave: String, val valor: String)

fun ContaMensal.entidades(): Pair<ContaEntidade, List<VersaoContaEntidade>> =
    ContaEntidade(id, inicio.toString()) to versoes.map { VersaoContaEntidade.de(id, it) }
