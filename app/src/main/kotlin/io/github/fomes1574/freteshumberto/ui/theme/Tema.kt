package io.github.fomes1574.freteshumberto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.fomes1574.freteshumberto.R

/** Paleta do protótipo (docs/ARQUITETURA.md §4). Não trocar sem pedido do usuário. */
object Cores {
    val Fundo = Color(0xFFF3F0E8)
    val Cartao = Color(0xFFFFFFFF)
    val Tinta = Color(0xFF191D1B)
    val Apagado = Color(0xFF565D59)
    val Linha = Color(0xFFDDD7CA)
    val Verde = Color(0xFF17603F)
    val VerdeClaro = Color(0xFFDCEFE3)
    val Combustivel = Color(0xFFA8500B)
    val CombustivelClaro = Color(0xFFFBE6D1)
    val Negativo = Color(0xFFA9221B)
    val Azul = Color(0xFF1F4E79)
}

val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private val esquema = lightColorScheme(
    primary = Cores.Verde,
    onPrimary = Color.White,
    primaryContainer = Cores.VerdeClaro,
    onPrimaryContainer = Cores.Verde,
    secondary = Cores.Combustivel,
    secondaryContainer = Cores.CombustivelClaro,
    background = Cores.Fundo,
    onBackground = Cores.Tinta,
    surface = Cores.Cartao,
    onSurface = Cores.Tinta,
    surfaceVariant = Color(0xFFE7E3D8),
    onSurfaceVariant = Cores.Apagado,
    outline = Cores.Linha,
    error = Cores.Negativo,
)

/**
 * Corpo ~18sp e valor principal ~46sp. Usar sp sempre: o Android aplica a escala de fonte
 * do sistema, e o A/A+/A++ dos Ajustes multiplica por cima (etapa 3).
 */
private fun tipografia(escala: Float): Typography {
    fun s(tam: Int, peso: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = Atkinson, fontWeight = peso, fontSize = (tam * escala).sp, lineHeight = (tam * escala * 1.3f).sp)
    return Typography(
        displayLarge = s(46, FontWeight.Bold),
        headlineMedium = s(22, FontWeight.Bold),
        titleLarge = s(20, FontWeight.Bold),
        titleMedium = s(18, FontWeight.Bold),
        bodyLarge = s(18),
        bodyMedium = s(16),
        labelLarge = s(18, FontWeight.Bold),
        labelMedium = s(15, FontWeight.Bold),
        labelSmall = s(14, FontWeight.Bold),
    )
}

/** [escalaLetra]: 1.0 (A), 20/18 (A+), 23/18 (A++). */
@Composable
fun TemaMeusFretes(escalaLetra: Float = 1f, conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia(escalaLetra), content = conteudo)
}
