plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun env(name: String, fallback: String = "") = System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

// Supabase URL + anon (publishable) key: env vars, or supabase.properties in the repo root.
// The anon key is public by design; Row Level Security (supabase/schema.sql) protects the data.
val supa = java.util.Properties().apply {
    val f = rootProject.file("supabase.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun supa(name: String) = env(name, supa.getProperty(name, "").trim())

android {
    namespace = "dev.imhamid.period"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.imhamid.period"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${supa("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supa("SUPABASE_KEY")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"imhamiddev/Period\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("signing/luna.jks")
            storePassword = env("KEYSTORE_PASSWORD", "lJXwsC4QlP54Hgaj0UCs")
            keyAlias = env("KEY_ALIAS", "luna")
            keyPassword = env("KEY_PASSWORD", "lJXwsC4QlP54Hgaj0UCs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
