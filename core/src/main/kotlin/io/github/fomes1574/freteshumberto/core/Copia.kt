package io.github.fomes1574.freteshumberto.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.time.LocalDate
import java.time.YearMonth
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Cópia recusada. [mensagem] já está pronta para mostrar à pessoa. */
class CopiaInvalida(val mensagem: String) : Exception(mensagem)

/**
 * Cópia de segurança (arquivo `.fretes`, R09). Formato JSON versionado, com tudo o que o banco
 * guarda: lançamentos e IDs, metas históricas, contas com versões, ocorrências (inclusive meses
 * pulados e exceções), revisões e conferências, e os ajustes.
 *
 * Com senha: AES-256-GCM, chave por PBKDF2-SHA256 com 210 mil iterações, sal de 16 bytes e
 * IV de 12 bytes (os mesmos parâmetros do protótipo). A senha não é guardada.
 *
 * Tudo é validado antes de substituir o banco; qualquer problema vira [CopiaInvalida].
 */
object Copia {
    const val FORMATO = "meus-fretes"
    const val FORMATO_CIFRADO = "meus-fretes-cifrado"
    const val VERSAO = 1
    const val ITERACOES = 210_000
    private const val MAX_LANCAMENTOS = 200_000
    private const val MAX_CONTAS = 500

    private val json = Json { prettyPrint = false }

    // ---------------------------------------------------------------- gravar

    fun paraJson(e: Estado, criadoEm: String): String {
        val raiz = buildJsonObject {
            put("formato", FORMATO)
            put("versao", VERSAO)
            put("criadoEm", criadoEm)
            put("lancamentos", buildJsonArray { e.lancamentos.forEach { add(lancamento(it)) } })
            put("metasDiarias", buildJsonArray {
                e.metas.diarias.sortedBy { it.desde }.forEach { add(buildJsonObject { put("desde", it.desde.toString()); put("valor", it.valor) }) }
            })
            put("metasMensais", buildJsonObject { e.metas.mensais.toSortedMap().forEach { (m, v) -> put(m.toString(), v) } })
            put("metaMensalBase", e.metas.baseMensal)
            put("contas", buildJsonArray { e.contas.forEach { add(conta(it)) } })
            put("ocorrencias", buildJsonArray { e.diario.ocorrencias.values.sortedBy { it.chave }.forEach { add(ocorrencia(it)) } })
            put("revisoes", buildJsonObject { e.diario.revisoes.toSortedMap().forEach { (d, v) -> put(d.toString(), v) } })
            put("conferidos", buildJsonObject { e.diario.conferidos.toSortedMap().forEach { (d, v) -> put(d.toString(), v) } })
            put("ajustes", buildJsonObject {
                put("kmPorLitro", e.ajustes.kmPorLitroCentesimos)
                put("precoManual", buildJsonObject {
                    put("data", e.ajustes.precoManual.data.toString())
                    put("preco", e.ajustes.precoManual.preco)
                    put("carimbo", e.ajustes.precoManual.carimbo)
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), raiz)
    }

    private fun lancamento(l: Lancamento) = buildJsonObject {
        put("id", l.id)
        put("tipo", l.tipo.name)
        put("data", l.data.toString())
        put("valor", l.valor)
        put("descricao", l.descricao)
        l.kmCentesimos?.let { put("km", it) }
        l.minutos?.let { put("minutos", it) }
        l.litrosCentesimos?.let { put("litros", it) }
        l.precoLitro?.let { put("precoLitro", it) }
        l.modoAbastecimento?.let { put("modo", it.name) }
        l.categoria?.let { put("categoria", it) }
        l.ocorrencia?.let { put("ocorrencia", it) }
        put("criadoEm", l.criadoEm)
        l.editadoEm?.let { put("editadoEm", it) }
    }

    private fun conta(c: ContaMensal) = buildJsonObject {
        put("id", c.id)
        put("inicio", c.inicio.toString())
        put("versoes", buildJsonArray {
            c.versoes.sortedBy { it.desde }.forEach { v ->
                add(buildJsonObject {
                    put("desde", v.desde.toString())
                    put("nome", v.nome)
                    put("valor", v.valor)
                    put("dia", v.dia)
                    put("ativa", v.ativa)
                    put("categoria", v.categoria)
                    v.fim?.let { put("fim", it.toString()) }
                })
            }
        })
    }

    private fun ocorrencia(o: Ocorrencia) = buildJsonObject {
        put("conta", o.contaId)
        put("mes", o.mes.toString())
        put("status", o.status.name)
        o.lancamentoId?.let { put("lancamento", it) }
        o.alteracao?.let { a ->
            put("alteracao", buildJsonObject {
                put("data", a.data.toString())
                put("valor", a.valor)
                put("descricao", a.descricao)
                put("categoria", a.categoria)
            })
        }
    }

    // ---------------------------------------------------------------- ler e validar

    /** Lê e valida uma cópia sem senha. Não toca no banco. */
    fun deJson(conteudo: String): Estado {
        val raiz = try {
            json.parseToJsonElement(conteudo) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: throw CopiaInvalida("O arquivo não é uma cópia do Meus Fretes ou está incompleto.")
        if (texto(raiz, "formato") == FORMATO_CIFRADO) throw CopiaInvalida("Esta cópia tem senha.")
        if (texto(raiz, "formato") != FORMATO) throw CopiaInvalida("O arquivo não é uma cópia do Meus Fretes.")
        val versao = (raiz["versao"] as? JsonPrimitive)?.intOrNull ?: throw CopiaInvalida("Cópia sem versão.")
        if (versao > VERSAO) throw CopiaInvalida("Esta cópia foi feita por uma versão mais nova do app. Atualize o app antes de restaurar.")
        if (versao < 1) throw CopiaInvalida("Versão da cópia desconhecida.")
        return try {
            ler(raiz)
        } catch (e: CopiaInvalida) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw CopiaInvalida("A cópia tem dados inválidos: ${e.message ?: "valor fora do esperado"}.")
        } catch (e: RuntimeException) {
            throw CopiaInvalida("A cópia tem dados inválidos.")
        }
    }

    private fun ler(raiz: JsonObject): Estado {
        val lancs = lista(raiz, "lancamentos")
        if (lancs.size > MAX_LANCAMENTOS) throw CopiaInvalida("A cópia tem lançamentos demais.")
        val ids = HashSet<String>()
        val lancamentos = lancs.map { el ->
            val o = obj(el, "lançamento")
            val id = texto(o, "id") ?: throw CopiaInvalida("Lançamento sem identificação.")
            if (id.isBlank() || id.length > 100 || !ids.add(id)) throw CopiaInvalida("Lançamento repetido ou sem identificação.")
            Lancamento(
                id = id,
                tipo = enumOu<Tipo>(texto(o, "tipo")) ?: throw CopiaInvalida("Tipo de lançamento inválido."),
                data = data(texto(o, "data")),
                valor = longo(o, "valor") ?: throw CopiaInvalida("Lançamento sem valor."),
                descricao = texto(o, "descricao") ?: "",
                kmCentesimos = longo(o, "km"),
                minutos = o["minutos"]?.let { (it as? JsonPrimitive)?.intOrNull ?: throw CopiaInvalida("Duração inválida.") },
                litrosCentesimos = longo(o, "litros"),
                precoLitro = longo(o, "precoLitro"),
                modoAbastecimento = texto(o, "modo")?.let { enumOu<ModoAbastecimento>(it) ?: throw CopiaInvalida("Abastecimento inválido.") },
                categoria = texto(o, "categoria"),
                ocorrencia = texto(o, "ocorrencia"),
                criadoEm = texto(o, "criadoEm") ?: "",
                editadoEm = texto(o, "editadoEm"),
            )
        }

        val diarias = lista(raiz, "metasDiarias").map { el ->
            val o = obj(el, "meta")
            MetaDiaria(data(texto(o, "desde")), positivo(longo(o, "valor"), "Meta inválida."))
        }
        if (diarias.isEmpty()) throw CopiaInvalida("A cópia não tem meta diária.")
        if (diarias.map { it.desde }.toSet().size != diarias.size) throw CopiaInvalida("Meta diária repetida.")
        val mensais = (raiz["metasMensais"] as? JsonObject ?: throw CopiaInvalida("Metas mensais ausentes.")).map { (k, v) ->
            mes(k) to naoNegativo((v as? JsonPrimitive)?.longOrNull, "Meta mensal inválida.")
        }.toMap()
        val metas = Metas(diarias.sortedBy { it.desde }, mensais, naoNegativo(longo(raiz, "metaMensalBase"), "Meta mensal inválida."))

        val contasJson = lista(raiz, "contas")
        if (contasJson.size > MAX_CONTAS) throw CopiaInvalida("A cópia tem contas do mês demais.")
        val contaIds = HashSet<String>()
        val contas = contasJson.map { el ->
            val o = obj(el, "conta")
            val id = texto(o, "id") ?: throw CopiaInvalida("Conta do mês sem identificação.")
            if (!contaIds.add(id)) throw CopiaInvalida("Conta do mês repetida.")
            val versoes = lista(o, "versoes").map { ve ->
                val v = obj(ve, "versão")
                VersaoConta(
                    desde = mes(texto(v, "desde")),
                    nome = texto(v, "nome") ?: throw CopiaInvalida("Conta do mês sem nome."),
                    valor = positivo(longo(v, "valor"), "Conta do mês inválida."),
                    dia = (v["dia"] as? JsonPrimitive)?.intOrNull ?: throw CopiaInvalida("Conta do mês sem dia."),
                    ativa = (v["ativa"] as? JsonPrimitive)?.booleanOrNull ?: throw CopiaInvalida("Conta do mês inválida."),
                    categoria = texto(v, "categoria") ?: CATEGORIA_CONTA,
                    fim = texto(v, "fim")?.let { data(it) },
                )
            }
            if (versoes.isEmpty() || versoes.map { it.desde }.toSet().size != versoes.size) throw CopiaInvalida("Versões da conta do mês inválidas.")
            ContaMensal(id, data(texto(o, "inicio")), versoes.sortedBy { it.desde })
        }

        val ocorrencias = lista(raiz, "ocorrencias").map { el ->
            val o = obj(el, "ocorrência")
            val alt = (o["alteracao"] as? JsonObject)?.let { a ->
                AlteracaoMes(data(texto(a, "data")), positivo(longo(a, "valor"), "Alteração mensal inválida."),
                    texto(a, "descricao")?.takeIf { it.length <= 160 } ?: throw CopiaInvalida("Alteração mensal inválida."),
                    texto(a, "categoria") ?: throw CopiaInvalida("Alteração mensal inválida."))
            }
            Ocorrencia(
                contaId = texto(o, "conta") ?: throw CopiaInvalida("Ocorrência sem conta."),
                mes = mes(texto(o, "mes")),
                status = enumOu<StatusOcorrencia>(texto(o, "status")) ?: throw CopiaInvalida("Ocorrência inválida."),
                lancamentoId = texto(o, "lancamento"),
                alteracao = alt,
            )
        }
        val occ = LinkedHashMap<String, Ocorrencia>()
        val porId = lancamentos.associateBy { it.id }
        for (o in ocorrencias) {
            if (o.contaId !in contaIds) throw CopiaInvalida("Ocorrência de uma conta que não existe.")
            if (occ.put(o.chave, o) != null) throw CopiaInvalida("Ocorrência repetida no mesmo mês.")
            if (o.status == StatusOcorrencia.GERADA && o.lancamentoId == null) throw CopiaInvalida("Ocorrência sem a despesa lançada.")
            if (o.lancamentoId != null && porId[o.lancamentoId]?.ocorrencia != o.chave) throw CopiaInvalida("Ocorrência não confere com a despesa.")
        }
        for (l in lancamentos) {
            if (l.ocorrencia != null && occ[l.ocorrencia]?.lancamentoId != l.id) throw CopiaInvalida("Despesa mensal sem ocorrência correspondente.")
            if (l.ocorrencia != null && l.tipo != Tipo.DESPESA) throw CopiaInvalida("Só despesas podem vir de conta do mês.")
        }

        val revisoes = mapaDias(raiz, "revisoes")
        val conferidos = mapaDias(raiz, "conferidos")

        val aj = raiz["ajustes"] as? JsonObject ?: throw CopiaInvalida("Ajustes ausentes.")
        val pm = aj["precoManual"] as? JsonObject ?: throw CopiaInvalida("Referência de preço inválida.")
        val ajustes = Ajustes(
            kmPorLitroCentesimos = positivo(longo(aj, "kmPorLitro"), "Consumo inválido."),
            precoManual = Combustivel.ReferenciaManual(data(texto(pm, "data")), positivo(longo(pm, "preco"), "Referência de preço inválida."), texto(pm, "carimbo") ?: ""),
        )
        return Estado(Diario(lancamentos, occ, revisoes, conferidos), contas, metas, ajustes)
    }

    private fun mapaDias(raiz: JsonObject, campo: String): Map<LocalDate, Int> =
        (raiz[campo] as? JsonObject ?: throw CopiaInvalida("Campo $campo ausente.")).map { (k, v) ->
            val n = (v as? JsonPrimitive)?.intOrNull ?: throw CopiaInvalida("Revisão inválida.")
            if (n < 0) throw CopiaInvalida("Revisão inválida.")
            data(k) to n
        }.toMap()

    private fun texto(o: JsonObject, campo: String): String? {
        val v = o[campo] ?: return null
        if (v is JsonNull) return null
        val p = v as? JsonPrimitive ?: throw CopiaInvalida("Campo $campo inválido.")
        if (!p.isString) throw CopiaInvalida("Campo $campo inválido.")
        return p.content
    }

    private fun longo(o: JsonObject, campo: String): Long? {
        val v = o[campo] ?: return null
        if (v is JsonNull) return null
        val p = v as? JsonPrimitive
        if (p == null || p.isString) throw CopiaInvalida("Campo $campo inválido.")
        return p.longOrNull ?: throw CopiaInvalida("Campo $campo inválido.")
    }

    private fun lista(o: JsonObject, campo: String): JsonArray = o[campo] as? JsonArray ?: throw CopiaInvalida("Campo $campo ausente.")

    private fun obj(e: JsonElement, nome: String): JsonObject = e as? JsonObject ?: throw CopiaInvalida("Registro de $nome inválido.")

    private fun data(s: String?): LocalDate = try {
        LocalDate.parse(s ?: throw CopiaInvalida("Data ausente."))
    } catch (_: java.time.format.DateTimeParseException) {
        throw CopiaInvalida("Data inválida: $s.")
    }

    private fun mes(s: String?): YearMonth = try {
        YearMonth.parse(s ?: throw CopiaInvalida("Mês ausente."))
    } catch (_: java.time.format.DateTimeParseException) {
        throw CopiaInvalida("Mês inválido: $s.")
    }

    private fun positivo(v: Long?, msg: String): Long = v?.takeIf { it in 1..Dinheiro.MAX } ?: throw CopiaInvalida(msg)

    private fun naoNegativo(v: Long?, msg: String): Long = v?.takeIf { it in 0..Dinheiro.MAX } ?: throw CopiaInvalida(msg)

    private inline fun <reified T : Enum<T>> enumOu(s: String?): T? = enumValues<T>().firstOrNull { it.name == s }

    // ---------------------------------------------------------------- senha

    /** Cifra o JSON da cópia. Sem senha, a cópia fica legível (a tela avisa). */
    fun cifrar(conteudo: String, senha: String, aleatorio: SecureRandom = SecureRandom()): String {
        require(senha.isNotEmpty())
        val sal = ByteArray(16).also { aleatorio.nextBytes(it) }
        val iv = ByteArray(12).also { aleatorio.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, chave(senha, sal, ITERACOES), GCMParameterSpec(128, iv))
        val dados = c.doFinal(conteudo.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        val raiz = buildJsonObject {
            put("formato", FORMATO_CIFRADO)
            put("versao", VERSAO)
            put("kdf", "PBKDF2-SHA256")
            put("iteracoes", ITERACOES)
            put("sal", b64.encodeToString(sal))
            put("iv", b64.encodeToString(iv))
            put("dados", b64.encodeToString(dados))
        }
        return json.encodeToString(JsonObject.serializer(), raiz)
    }

    /** O arquivo pede senha? */
    fun temSenha(arquivo: String): Boolean = try {
        (json.parseToJsonElement(arquivo) as? JsonObject)?.let { texto(it, "formato") == FORMATO_CIFRADO } ?: false
    } catch (_: Exception) {
        false
    }

    /** Abre o conteúdo de um arquivo `.fretes`, com ou sem senha, e valida tudo. */
    fun abrir(arquivo: String, senha: String?): Estado {
        if (!temSenha(arquivo)) return deJson(arquivo)
        if (senha.isNullOrEmpty()) throw CopiaInvalida("Esta cópia tem senha. Digite a senha para restaurar.")
        val raiz = json.parseToJsonElement(arquivo) as JsonObject
        val versao = (raiz["versao"] as? JsonPrimitive)?.intOrNull ?: throw CopiaInvalida("Cópia sem versão.")
        if (versao > VERSAO) throw CopiaInvalida("Esta cópia foi feita por uma versão mais nova do app. Atualize o app antes de restaurar.")
        val iter = (raiz["iteracoes"] as? JsonPrimitive)?.intOrNull
        if (texto(raiz, "kdf") != "PBKDF2-SHA256" || iter == null || iter !in 100_000..5_000_000) throw CopiaInvalida("Cópia com senha em formato desconhecido.")
        val b64 = Base64.getDecoder()
        val (sal, iv, dados) = try {
            Triple(b64.decode(texto(raiz, "sal")), b64.decode(texto(raiz, "iv")), b64.decode(texto(raiz, "dados")))
        } catch (_: IllegalArgumentException) {
            throw CopiaInvalida("O arquivo da cópia está danificado.")
        }
        if (sal.size != 16 || iv.size != 12) throw CopiaInvalida("O arquivo da cópia está danificado.")
        val claro = try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, chave(senha, sal, iter), GCMParameterSpec(128, iv))
            String(c.doFinal(dados), Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            throw CopiaInvalida("Senha incorreta ou arquivo alterado.")
        } catch (_: java.security.GeneralSecurityException) {
            throw CopiaInvalida("Senha incorreta ou arquivo alterado.")
        }
        return deJson(claro)
    }

    private fun chave(senha: String, sal: ByteArray, iteracoes: Int): SecretKeySpec {
        val spec = PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** Nome sugerido no seletor de arquivos. */
    fun nomeArquivo(hoje: LocalDate) = "meus-fretes-$hoje.fretes"
}
