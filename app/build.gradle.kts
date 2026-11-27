import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun env(name: String, fallback: String = "") = System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

// Supabase URL + anon (publishable) key: env vars, or supabase.properties in the repo root.
// The anon key is public by design; Row Level Security (supabase/schema.sql) protects the data.
val supa = Properties().apply {
    val f = rootProject.file("supabase.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun supa(name: String) = env(name, supa.getProperty(name, "").trim())

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

android {
    namespace = "dev.imhamid.period"
    compileSdk = 36

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

    // Release signing: the keystore and its passwords never live in the repo.
    //   KEYSTORE_FILE     path to the .jks (defaults to signing/luna.jks, which is git-ignored)
    //   KEYSTORE_PASSWORD, KEY_ALIAS (default "luna"), KEY_PASSWORD (defaults to KEYSTORE_PASSWORD)
    // Without them (e.g. a fresh clone) release builds fall back to the debug key, so they still build
    // but can't be installed over a properly signed release.
    signingConfigs {
        create("release") {
            val ks = env("KEYSTORE_FILE").takeIf { it.isNotBlank() }?.let { file(it) }
                ?: rootProject.file("signing/luna.jks")
            val pass = env("KEYSTORE_PASSWORD")
            if (ks.exists() && pass.isNotBlank()) {
                storeFile = ks
                storePassword = pass
                keyAlias = env("KEY_ALIAS", "luna")
                keyPassword = env("KEY_PASSWORD", pass)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
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
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
