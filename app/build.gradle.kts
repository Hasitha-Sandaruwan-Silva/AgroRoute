import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val mapTilerApiKey = localConfig.getProperty("MAPTILER_API_KEY", "").trim()
val mapTilerKeyLiteral = "\"" + mapTilerApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\r", "\\r").replace("\n", "\\n") + "\""

android {
    namespace = "com.agroroute"
    compileSdk = 35

    ksp {
        arg("kotlin.ksp.suppressKotlinVersionMismatch", "true")
    }

    defaultConfig {
        applicationId = "com.agroroute"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MAPTILER_API_KEY", mapTilerKeyLiteral)

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }

}

dependencies {
    // ── Compose BOM ────────────────────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // ── AndroidX ───────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Required on the compile classpath for CameraProvider's ListenableFuture API.
    implementation("com.google.guava:guava:33.3.1-android")
    val cameraXVersion = "1.5.3"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Navigation Compose ─────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Firebase BOM ──────────────────────────────────────────────────────────
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // ── Kotlin Coroutines ─────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Coil (image loading) ──────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Gson (Room TypeConverter for List<String>) ────────────────────────────
    implementation(libs.gson)

    // ── Lucide Icons ───────────────────────────────────────────────────────────
    // Using local Compose Material Icons mapping in com.github.thelacspace.icons.Lucide

    // ── OkHttp (Cloudinary unsigned multipart upload — no API secret) ─────────
    implementation(libs.okhttp)
    implementation("org.maplibre.gl:android-sdk:11.8.0")

}
