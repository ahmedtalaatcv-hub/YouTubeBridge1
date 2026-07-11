plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.youtubebridge.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.youtubebridge.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // --- Core / Jetpack ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Compose (Material 3) ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Embedded HTTP Server (pure Java/Kotlin, no Node/Python) ---
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // --- YouTube stream extraction (native Kotlin/Java, no Python/yt-dlp needed) ---
    // NewPipeExtractor: a pure Java/Kotlin library used by the NewPipe app to parse
    // YouTube's internal player API and resolve direct playable stream URLs.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.3")
    implementation("org.jsoup:jsoup:1.17.2") // required by NewPipeExtractor for HTML parsing

    // --- QR Code generation (native, no camera libs needed) ---
    implementation("com.google.zxing:core:3.5.3")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- JSON ---
    implementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
