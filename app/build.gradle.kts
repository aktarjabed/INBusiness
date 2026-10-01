plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

kapt {
    arguments {
        // Schema export is opt-in (`-ProomSchemaExport=true`, used by the CI schema job). When a
        // schema file for the current version already exists, Room's processor *deserializes* it
        // through kotlinx-serialization on the annotation-processor classpath. That classpath is
        // not covered by the version pin below, and Room 2.8.5 ships serializers compiled against
        // kotlinx-serialization 1.7.x while its POM pulls 1.8.1 - deserialization therefore crashes
        // with `AbstractMethodError: FieldBundle$$serializer ... typeParametersSerializers()`.
        // Ordinary builds (app + instrumented tests) do not need to re-export the schema; they read
        // the checked-in JSON from `app/schemas`, which is also what the migration tests consume.
        // CI regenerates and diffs it in the dedicated schema step, so drift is still caught.
        if (project.hasProperty("roomSchemaExport")) {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

android {
    namespace = "com.aktarjabed.inbusiness"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aktarjabed.inbusiness"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled  = true
            isShrinkResources= true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs.useLegacyPackaging = false
    }
    kotlinOptions.jvmTarget = "17"
    buildFeatures.compose = true
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest").apply {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

dependencies {
    // Bumped from 2024.05.00 in step with the Vico 2.0.3 chart library, which is built
    // against BOM 2025.01.00; leaving the BOM behind would have let Gradle silently mix
    // 1.6.x runtime artifacts with the 1.7.x ones Vico pulls in.
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Stable release: the previous 2.8.0-beta01 was a beta in a production path.
    implementation("androidx.navigation:navigation-compose:2.8.9")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    // Lifecycle-aware collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")
    androidTestImplementation("androidx.room:room-testing:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.58")
    kapt("com.google.dagger:hilt-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Charts. Vico 2.x: DashboardScreen is written against the 2.x Compose API
    // (compose.cartesian.* / core.cartesian.data.*), so 1.14.0 could never compile.
    implementation("com.patrykandpatrick.vico:compose:2.0.3")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Security & Crypto
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("net.zetetic:sqlcipher-android:4.19.1")
    implementation("androidx.sqlite:sqlite:2.7.0")
    implementation("androidx.sqlite:sqlite-ktx:2.7.0")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Testing
    // Mockito 5's inline mock maker runs on the JVM, where mocking Kotlin final classes works
    // (see QuotaGateTest). On-device the subclass mock maker is used and cannot do this.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // No Mockito here on purpose: the Android runner uses Dexmaker's subclass mock maker, which
    // cannot mock final Kotlin classes, so any instrumented mock of QuotaGate/DeviceClassifier
    // fails in @Before. Instrumented tests use the real collaborators instead.
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// androidx.room:room-migration:2.8.5 declares kotlinx-serialization-json:1.8.1, but its bundled
// migration serializers (`FieldBundle$$serializer` and friends) are compiled against 1.7.x. In
// 1.8.0 `GeneratedSerializer` gained an abstract `typeParametersSerializers()`, so resolving 1.8.x
// makes Room's serializers throw `AbstractMethodError` when `MigrationTestHelper` reads an exported
// schema bundle - every migration test crashes. Pinning to the version Room was actually built
// against restores those serializers; nothing in this app uses kotlinx-serialization directly, and
// the only other consumer (navigation-common 2.8.9) declares 1.6.3.
configurations.configureEach {
    // Annotation-processor/kapt classpaths are deliberately excluded: Room's schema-export code
    // runs there and is sensitive to exactly which serialization classes it loads (see the kapt
    // block above). Forcing versions into those classpaths is what broke `:app:kaptDebugKotlin`,
    // so this pin covers only the app's compile/runtime/test classpaths - where the mismatch
    // actually surfaced as an AbstractMethodError inside MigrationTestHelper.
    if (!name.contains("kapt", ignoreCase = true) &&
        !name.contains("annotationProcessor", ignoreCase = true)
    ) {
        resolutionStrategy {
            force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
    }
}
