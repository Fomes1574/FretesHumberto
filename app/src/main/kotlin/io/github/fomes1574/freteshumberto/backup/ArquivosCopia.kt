package io.github.fomes1574.freteshumberto.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * Leitura e gravação dos arquivos `.fretes` escolhidos no seletor do Android (sem permissão
 * de armazenamento). A cópia automática de antes de uma restauração fica na área privada do app.
 */
class ArquivosCopia(private val context: Context) {
    private val preventiva get() = File(context.filesDir, "copias/antes-da-restauracao.fretes")

    /**
     * Grava e confere: lê de volta e compara. Só termina sem exceção se o arquivo
     * escolhido ficou com o conteúdo completo.
     */
    fun escrever(destino: Uri, conteudo: String) {
        val bytes = conteudo.toByteArray(Charsets.UTF_8)
        val r = context.contentResolver
        val saida = try {
            r.openOutputStream(destino, "wt")
        } catch (e: IllegalArgumentException) {
            r.openOutputStream(destino, "w")
        } catch (e: UnsupportedOperationException) {
            r.openOutputStream(destino, "w")
        } ?: throw IOException("Sem acesso ao arquivo")
        saida.use {
            it.write(bytes)
            it.flush()
        }
        val lido = try {
            r.openInputStream(destino)?.use { it.readBytes() }
        } catch (e: Exception) {
            // Alguns serviços (ex.: nuvem) não deixam ler na hora; a gravação já terminou sem erro.
            null
        }
        if (lido != null && !lido.contentEquals(bytes)) throw IOException("Arquivo gravado incompleto")
    }

    fun ler(origem: Uri): String {
        val entrada = context.contentResolver.openInputStream(origem) ?: throw IOException("Sem acesso ao arquivo")
        return entrada.use { s ->
            val bytes = s.readNBytesCompat(LIMITE + 1)
            if (bytes.size > LIMITE) throw IOException("Arquivo grande demais")
            String(bytes, Charsets.UTF_8)
        }
    }

    fun guardarPreventiva(conteudo: String) {
        val f = preventiva
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(conteudo, Charsets.UTF_8)
        if (!tmp.renameTo(f)) {
            f.delete()
            if (!tmp.renameTo(f)) throw IOException("Não foi possível guardar a cópia automática")
        }
    }

    fun lerPreventiva(): String? = preventiva.takeIf { it.exists() }?.readText(Charsets.UTF_8)

    fun temCopiaPreventiva(): Boolean = preventiva.exists()

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val saida = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0
        while (total < max) {
            val n = read(buf, 0, minOf(buf.size, max - total))
            if (n < 0) break
            saida.write(buf, 0, n)
            total += n
        }
        return saida.toByteArray()
    }

    private companion object {
        /** 50 MB: muito acima de anos de lançamentos. */
        const val LIMITE = 50 * 1024 * 1024
    }
}
