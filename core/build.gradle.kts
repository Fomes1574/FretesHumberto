// Regras puras do negócio (dinheiro, datas, lucro, metas, combustível, contas do mês, estrada).
// Sem Android: roda e testa na JVM em segundos -> ./gradlew :core:test
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
