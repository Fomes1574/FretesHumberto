package io.github.fomes1574.freteshumberto.backup

import android.content.Context
import android.net.Uri
import io.github.fomes1574.freteshumberto.core.Copia
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File
import java.io.IOException

/**
 * Leitura e gravação dos arquivos `.fretes` escolhidos no seletor do Android (sem permissão
 * de armazenamento). A cópia automática de antes de uma restauração fica na área privada do app.
 */
class ArquivosCopia(private val context: Context) {
    private val preventiva get() = File(context.filesDir, "copias/antes-da-restauracao.fretes")

    /**
     * Devolve true quando os bytes escritos puderam ser relidos e conferidos.
     */
    fun escrever(destino: Uri, conteudo: String): Boolean {
        val bytes = conteudo.toByteArray(Charsets.UTF_8)
        if (bytes.size > Copia.LIMITE_BYTES) throw IOException("Arquivo grande demais")
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
            r.openInputStream(destino)?.use { it.readNBytesCompat(bytes.size + 1) }
        } catch (e: Exception) {
            // Alguns serviços (ex.: nuvem) não deixam ler na hora; a gravação já terminou sem erro.
            null
        }
        if (lido != null && !lido.contentEquals(bytes)) throw IOException("Arquivo gravado incompleto")
        return lido != null
    }

    fun ler(origem: Uri): String {
        val entrada = context.contentResolver.openInputStream(origem) ?: throw IOException("Sem acesso ao arquivo")
        return entrada.use { s ->
            val bytes = s.readNBytesCompat(Copia.LIMITE_BYTES + 1)
            if (bytes.size > Copia.LIMITE_BYTES) throw IOException("Arquivo grande demais")
            String(bytes, Charsets.UTF_8)
        }
    }

    fun guardarPreventiva(conteudo: String) {
        val f = preventiva
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".tmp")
        val bytes = conteudo.toByteArray(Charsets.UTF_8)
        if (bytes.size > Copia.LIMITE_BYTES) throw IOException("Arquivo grande demais")
        try {
            tmp.writeBytes(bytes)
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            tmp.delete()
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

}
