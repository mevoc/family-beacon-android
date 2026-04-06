import java.io.File
import java.util.Properties

fun gitVersionName(): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val gitExe = if (isWindows) {
        val localAppData = System.getenv("LOCALAPPDATA") ?: ""
        val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
        listOf(
            "$programFiles\\Git\\cmd\\git.exe",
            "$programFiles\\Git\\bin\\git.exe",
            "$localAppData\\Programs\\Git\\cmd\\git.exe",
            "git.exe"
        ).firstOrNull { File(it).exists() } ?: "git.exe"
    } else "git"
    return try {
        val proc = ProcessBuilder(gitExe, "describe", "--tags", "--always", "--dirty=-dev")
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        output.ifEmpty { "dev" }
    } catch (e: Exception) {
        "dev"
    }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.mevoc.familybeacon"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mevoc.familybeacon"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = gitVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Maps API key: read from local.properties (local dev) or GOOGLE_MAPS_KEY env var (CI/release).
        // Falls back to empty string so builds succeed without a key (map picker won't function).
        val localProps = Properties().also { props ->
            rootProject.file("local.properties").takeIf { it.exists() }
                ?.inputStream()?.use { props.load(it) }
        }
        manifestPlaceholders["googleMapsKey"] =
            localProps.getProperty("GOOGLE_MAPS_KEY")
                ?: System.getenv("GOOGLE_MAPS_KEY")
                ?: ""
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}
