plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.aktarjabed.inbusiness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aktarjabed.inbusiness"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Read NIC credentials from local.properties
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
            // Configure signing from local.properties or environment variables
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
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // Baseline Profile
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Core Android & Jetpack
    // ═══════════════════════════════════════════════════════════════════

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Compose Animation
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Room Database (SQLCipher Encrypted)
    // ═══════════════════════════════════════════════════════════════════
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // SQLCipher for encrypted database
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Hilt Dependency Injection
    // ═══════════════════════════════════════════════════════════════════
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Coroutines
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: PDF Generation (iText 7)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.itextpdf:itext7-core:7.2.5")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: QR Code Generation (ZXing)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.google.zxing:core:3.5.3")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 1: Charts & Visualization (Vico)
    // ═══════════════════════════════════════════════════════════════════
    val vicoVersion = "1.15.0"
    implementation("com.patrykandpatrick.vico:compose:$vicoVersion")
    implementation("com.patrykandpatrick.vico:compose-m3:$vicoVersion")
    implementation("com.patrykandpatrick.vico:core:$vicoVersion")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Network Layer (Retrofit & OkHttp)
    // ═══════════════════════════════════════════════════════════════════
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Security & Encryption
    // ═══════════════════════════════════════════════════════════════════

    // BouncyCastle (CRITICAL for NIC encryption)
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    // Android Keystore & Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: AI & Machine Learning (Anomaly Detection)
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.apache.commons:commons-math3:3.6.1")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Kotlinx Serialization
    // ═══════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2: Performance - Baseline Profile
    // ═══════════════════════════════════════════════════════════════════
    baselineProfile(project(":baselineprofile"))
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // ═══════════════════════════════════════════════════════════════════
    // Testing
    // ═══════════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.google.truth:truth:1.4.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}