plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release sunucu adresi (debug ve testleri etkilemez)
val releaseApiBaseUrl: String? = providers.gradleProperty("rapidquiz.releaseApiBaseUrl").orNull
val releaseApiBaseUrlMissing = releaseApiBaseUrl.isNullOrBlank()

android {
    namespace = "com.busracankit.rapidquiz"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.busracankit.rapidquiz"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Emülatör → Mac'teki runserver. Gerçek cihaz ya da canlı için koda dokunmadan
            // ~/.gradle/gradle.properties veya komut satırında değiştirilebilir:
            //   rapidquiz.debugApiBaseUrl=http://192.168.1.20:8000/
            // (Gerçek cihazda IP'yi src/debug/res/xml/network_security_config.xml'e de ekle.)
            val debugBaseUrl = providers.gradleProperty("rapidquiz.debugApiBaseUrl").orNull ?: "http://10.0.2.2:8000/"
            buildConfigField("String", "API_BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            // Canlı sunucu yok: release için adres her seferinde açıkça verilir, koda alan adı gömülmez.
            //   ./gradlew assembleRelease -Prapidquiz.releaseApiBaseUrl=https://sunucu-adresi/
            // Verilmezse aşağıdaki kontrol release görevlerini anlaşılır bir mesajla durdurur.
            buildConfigField("String", "API_BASE_URL", "\"${releaseApiBaseUrl.orEmpty()}\"")
            // R8 (küçültme + optimizasyon). Kurallar: src/main/keepRules/rules.keep
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            // GEÇİCİ: release'i emülatörde/telefonda denemek için debug anahtarıyla imzalanır.
            // Google Play'e yüklemeden önce gerçek yükleme anahtarı (keystore) ile değiştirilmeli.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Adres verilmeden release APK/AAB üretilmesin (boş adresle uygulama açılışta çöker).
tasks.configureEach {
    val isReleaseOutput = name.endsWith("Release") &&
        listOf("assemble", "bundle", "install", "package").any { name.startsWith(it) }
    if (isReleaseOutput) {
        val missing = releaseApiBaseUrlMissing
        doFirst {
            check(!missing) {
                "Release için sunucu adresi gerekli: -Prapidquiz.releaseApiBaseUrl=https://sunucu-adresi/"
            }
        }
    }
}
