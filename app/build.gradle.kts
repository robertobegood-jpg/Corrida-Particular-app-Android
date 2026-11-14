import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // Prints automáticos das telas nos testes (revisão visual antes de cada versão)
    id("app.cash.paparazzi")
}

android {
    namespace = "com.corridaparticular.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.corridaparticular.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
    }

    // Assinatura oficial do app. A chave NÃO fica no repositório:
    // - no GitHub Actions vem dos secrets KEYSTORE_BASE64 e KEYSTORE_PASSWORD;
    // - no computador, coloque o arquivo em app/assinatura/corrida-particular.jks
    //   e a senha em keystore.properties (senha=...), ambos ignorados pelo git.
    // Sem a chave (ex.: quem clonar o projeto), o build usa a chave de debug padrão.
    val chave = file("assinatura/corrida-particular.jks")
    val senhaChave: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: rootProject.file("keystore.properties").takeIf { it.exists() }?.let { arq ->
            Properties().apply { arq.inputStream().use { load(it) } }.getProperty("senha")
        }
    val temAssinatura = chave.exists() && senhaChave != null

    signingConfigs {
        if (temAssinatura) {
            create("oficial") {
                storeFile = chave
                storePassword = senhaChave
                keyAlias = "androiddebugkey"
                keyPassword = senhaChave
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
        debug {
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Avisos de lint não devem impedir a geração do APK de release.
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
