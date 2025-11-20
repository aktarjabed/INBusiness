plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" // Phase 2
    id("androidx.baselineprofile") // Phase 2
}

android {
    namespace = "com.aktarjabed.inbusiness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aktarjabed.inbusiness"
        minSdk = 24
        targetSdk = 35
        versionCode = 2  // ← INCREMENT for Phase 2
        versionName = "2.0.0"  // ← Phase 2 version

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // ═══════════════════════════════════════════════════════════════
        // PHASE 2: NIC E-Invoicing Configuration
        // ═══════════════════════════════════════════════════════════════
        val localProperties = File(rootProject.projectDir, "local.properties")
        val properties = java.util.Properties()
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }

        buildConfigField("String", "NIC_USERNAME", "\"${properties.getProperty("NIC_USERNAME", "")}\"")
        buildConfigField("String", "NIC_PASSWORD", "\"${properties.getProperty("NIC_PASSWORD", "")}\"")
        buildConfigField("String", "NIC_BASE_URL", "\"${properties.getProperty("NIC_BASE_URL", "https://einv-apisandbox.nic.in")}\"")
        buildConfigField("boolean", "ENABLE_NIC", properties.getProperty("ENABLE_NIC", "false"))
    }

    signingConfigs {
        create("release") {
            val localProperties = File(rootProject.projectDir, "local.properties")
            val properties = java.util.Properties()
            if (localProperties.exists()) {
                properties.load(localProperties.inputStream())
            }

            storeFile = properties.getProperty("KEYSTORE_FILE")?.let { file(it) }
            storePassword = properties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = properties.getProperty("KEY_ALIAS")
            keyPassword = properties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // PHASE 2: Baseline Profile
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        // PHASE 2: Compiler optimizations
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true  // ← PHASE 2: Enable BuildConfig
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.16"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // PHASE 2: Additional exclusions for BouncyCastle
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Core Android & Jetpack
    // ═══════════════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Room Database + SQLCipher
    // ═══════════════════════════════════════════════════════════════════
    val roomVersion = "2.7.0-alpha10"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Hilt Dependency Injection
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: PDF Generation (iText)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.itextpdf:itext7-core:8.0.5")
    implementation("com.github.barteksc:android-pdf-viewer:2.8.2")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: QR Code
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Date & Time
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Charts (Vico)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.29")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: DataStore & Security
    // ═══════════════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Network Layer (Retrofit & OkHttp)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Security & Encryption (BouncyCastle)
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: AI Anomaly Detection (Apache Commons Math)
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.apache.commons:commons-math3:3.6.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Kotlinx Serialization
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Performance - Baseline Profile
    // ═══════════════════════════════════════════════════════════════════
    baselineProfile(project(":baselineprofile"))
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // ═══════════════════════════════════════════════════════════════════
    // Testing
    // ═══════════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}