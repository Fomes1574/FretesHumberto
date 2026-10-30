import java.util.Properties

// AGP 9 tem Kotlin embutido: NÃO aplicar org.jetbrains.kotlin.android aqui.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Assinatura de release: lida de keystore.properties (local, fora do Git) ou de variáveis
// de ambiente (GitHub Actions, a partir dos Secrets). Sem elas, só o build debug funciona.
val assinatura = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun segredo(chave: String, env: String): String? = assinatura.getProperty(chave) ?: System.getenv(env)

android {
    namespace = "io.github.fomes1574.freteshumberto"
    compileSdk = 36

    defaultConfig {
        // Definitivo: mudar depois impede atualizar por cima da versão instalada.
        applicationId = "io.github.fomes1574.freteshumberto"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val arquivo = segredo("storeFile", "FRETES_KEYSTORE_PATH")
        if (arquivo != null) {
            create("release") {
                storeFile = rootProject.file(arquivo)
                storePassword = segredo("storePassword", "FRETES_KEYSTORE_PASSWORD")
                keyAlias = segredo("keyAlias", "FRETES_KEY_ALIAS")
                keyPassword = segredo("keyPassword", "FRETES_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Esquema do banco exportado e versionado: base para escrever e testar migrações.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
}
