// Regras puras do negócio (dinheiro, datas, lucro, metas, combustível, contas do mês, estrada).
// Sem Android: roda e testa na JVM em segundos -> ./gradlew :core:test
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Só a biblioteca JSON (sem plugin de compilador): a cópia monta e valida o JSON à mão.
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
