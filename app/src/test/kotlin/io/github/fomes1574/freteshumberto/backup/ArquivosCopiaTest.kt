package io.github.fomes1574.freteshumberto.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArquivosCopiaTest {
    @Test fun `gravação só é conferida quando o conteúdo pode ser relido por inteiro`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://teste/copia")
        val resolver = shadowOf(contexto.contentResolver)
        val gravado = ByteArrayOutputStream()
        val arquivos = ArquivosCopia(contexto)
        resolver.registerOutputStream(uri, gravado)
        resolver.registerInputStreamSupplier(uri) { throw FileNotFoundException("Leitura indisponível") }
        assertFalse(arquivos.escrever(uri, "copia"))
        assertEquals("copia", gravado.toString(Charsets.UTF_8.name()))
        resolver.registerInputStream(uri, ByteArrayInputStream("incompleta".toByteArray()))
        assertTrue(runCatching { arquivos.escrever(uri, "copia") }.exceptionOrNull() is IOException)
        resolver.registerInputStream(uri, ByteArrayInputStream("copia".toByteArray()))
        assertTrue(arquivos.escrever(uri, "copia"))
    }

    @Test fun `copia preventiva pode ser atualizada e relida`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val arquivos = ArquivosCopia(contexto)
        arquivos.guardarPreventiva("primeira")
        arquivos.guardarPreventiva("segunda")
        assertEquals("segunda", arquivos.lerPreventiva())
    }
}
