import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// Firma de release (upload key). Nunca en el repo: se lee de keystore.properties en la raíz
// (ignorado por git) o de variables de entorno. Sin ellas, bundleRelease genera un AAB sin firmar
// (sirve para que CI verifique R8, no para subir a Play).
val releaseSigning: Map<String, String>? = run {
    val file = rootProject.file("keystore.properties")
    val props = Properties().apply { if (file.exists()) file.inputStream().use(::load) }
    fun value(key: String, env: String) = props.getProperty(key) ?: System.getenv(env)
    val values = mapOf(
        "storeFile" to value("storeFile", "JUNTADAMIGOS_STORE_FILE"),
        "storePassword" to value("storePassword", "JUNTADAMIGOS_STORE_PASSWORD"),
        "keyAlias" to value("keyAlias", "JUNTADAMIGOS_KEY_ALIAS"),
        "keyPassword" to value("keyPassword", "JUNTADAMIGOS_KEY_PASSWORD"),
    )
    if (values.values.all { !it.isNullOrBlank() }) values.mapValues { it.value!! } else null
}

android {
    namespace = "com.juntadamigos.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.juntadamigos.app"
        minSdk = 26
        targetSdk = 36
        // Play rechaza un versionCode repetido: subilo en cada AAB (o pasalo con -PversionCode=N).
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0.0"
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigning != null) signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

secrets {
    // Key real en local.properties; local.defaults.properties permite compilar sin ella.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Mapa de resultados. 6.4.x compila con Kotlin 2.0/2.1 (las 6.7+ ya traen stdlib 2.2).
    implementation("com.google.maps.android:maps-compose:6.4.4")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
