// Arquivo raiz: só declara os plugins. Cada módulo aplica o que usa.
plugins {
    alias(libs.plugins.android.application) apply false
    // Coloca o Kotlin 2.3.20 no classpath; o AGP 9 usa este KGP no Kotlin embutido do módulo :app.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
