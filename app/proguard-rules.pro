# ═══════════════════════════════════════════════════════════════════════════
# INBusiness Phase 2 - Complete ProGuard Configuration
# Production-Ready | Optimized | Security-Hardened
# ═══════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════════════════════════════════════════════
# General Configuration
# ═══════════════════════════════════════════════════════════════════════════
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-dontoptimize
-dontpreverify

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ═══════════════════════════════════════════════════════════════════════════
# Kotlin
# ═══════════════════════════════════════════════════════════════════════════
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
}

# ═══════════════════════════════════════════════════════════════════════════
# Kotlinx Coroutines
# ═══════════════════════════════════════════════════════════════════════════
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ═══════════════════════════════════════════════════════════════════════════
# Kotlinx Serialization
# ═══════════════════════════════════════════════════════════════════════════
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aktarjabed.inbusiness.**$$serializer { *; }
-keepclassmembers class com.aktarjabed.inbusiness.** {
    *** Companion;
}
-keepclasseswithmembers class com.aktarjabed.inbusiness.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ═══════════════════════════════════════════════════════════════════════════
# Jetpack Compose
# ═══════════════════════════════════════════════════════════════════════════
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# Keep Compose compiler generated classes
-keep class androidx.compose.runtime.ComposerKt { *; }

# ═══════════════════════════════════════════════════════════════════════════
# Room Database
# ═══════════════════════════════════════════════════════════════════════════
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** Companion;
}
-dontwarn androidx.room.paging.**

# Keep all Room-generated classes
-keep class com.aktarjabed.inbusiness.data.database.AppDatabase_Impl { *; }
-keep class com.aktarjabed.inbusiness.data.dao.** { *; }

# ═══════════════════════════════════════════════════════════════════════════
# Hilt / Dagger
# ═══════════════════════════════════════════════════════════════════════════
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep Hilt generated components
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ═══════════════════════════════════════════════════════════════════════════
# Data Models & Entities (Phase 2)
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.aktarjabed.inbusiness.data.entities.** { *; }
-keepclassmembers class com.aktarjabed.inbusiness.data.entities.** {
    <fields>;
    <methods>;
}

# NIC API Models
-keep class com.aktarjabed.inbusiness.data.remote.models.** { *; }
-keepclassmembers class com.aktarjabed.inbusiness.data.remote.models.** {
    <fields>;
    <methods>;
}

# Domain Models
-keep class com.aktarjabed.inbusiness.domain.models.** { *; }
-keepclassmembers class com.aktarjabed.inbusiness.domain.models.** {
    <fields>;
}

# ═══════════════════════════════════════════════════════════════════════════
# Retrofit & OkHttp (NIC API Integration)
# ═══════════════════════════════════════════════════════════════════════════
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# ═══════════════════════════════════════════════════════════════════════════
# Gson (JSON Serialization)
# ═══════════════════════════════════════════════════════════════════════════
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ═══════════════════════════════════════════════════════════════════════════
# BouncyCastle (CRITICAL for NIC Encryption)
# ═══════════════════════════════════════════════════════════════════════════
-keep class org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

# Keep BouncyCastle provider
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keepclassmembers class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }

# Keep crypto algorithms
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.crypto.** { *; }

# ═══════════════════════════════════════════════════════════════════════════
# Apache Commons Math (AI Anomaly Detection)
# ═══════════════════════════════════════════════════════════════════════════
-keep class org.apache.commons.math3.** { *; }
-keepclassmembers class org.apache.commons.math3.** {
    <fields>;
    <methods>;
}
-dontwarn org.apache.commons.math3.**

# ═══════════════════════════════════════════════════════════════════════════
# iText PDF Generation
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.itextpdf.** { *; }
-keepclassmembers class com.itextpdf.** {
    <fields>;
    <methods>;
}
-dontwarn com.itextpdf.**
-dontwarn javax.xml.stream.**

# ═══════════════════════════════════════════════════════════════════════════
# ZXing (QR Code Generation)
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ═══════════════════════════════════════════════════════════════════════════
# Security Crypto (Encrypted SharedPreferences)
# ═══════════════════════════════════════════════════════════════════════════
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** {
    <fields>;
    <methods>;
}

# ═══════════════════════════════════════════════════════════════════════════
# SQLCipher (Encrypted Database)
# ═══════════════════════════════════════════════════════════════════════════
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** {
    <fields>;
    <methods>;
}
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ═══════════════════════════════════════════════════════════════════════════
# Baseline Profile (Performance Optimization)
# ═══════════════════════════════════════════════════════════════════════════
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# ═══════════════════════════════════════════════════════════════════════════
# Vico Charts (Dashboard Analytics)
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ═══════════════════════════════════════════════════════════════════════════
# Enums (Preserve valueOf and values methods)
# ═══════════════════════════════════════════════════════════════════════════
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ═══════════════════════════════════════════════════════════════════════════
# Serializable Classes
# ═══════════════════════════════════════════════════════════════════════════
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ═══════════════════════════════════════════════════════════════════════════
# Remove Logging in Release (Performance & Security)
# ═══════════════════════════════════════════════════════════════════════════
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ═══════════════════════════════════════════════════════════════════════════
# Keep BuildConfig
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.aktarjabed.inbusiness.BuildConfig { *; }

# ═══════════════════════════════════════════════════════════════════════════
# ViewModels (Hilt)
# ═══════════════════════════════════════════════════════════════════════════
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep ViewModel SavedStateHandle constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ═══════════════════════════════════════════════════════════════════════════
# Crash Reporting (Keep stack trace info)
# ═══════════════════════════════════════════════════════════════════════════
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class * extends java.lang.Throwable {
    <init>(...);
}

# ═══════════════════════════════════════════════════════════════════════════
# R8 Full Mode Compatibility
# ═══════════════════════════════════════════════════════════════════════════
-allowaccessmodification
-repackageclasses

# ═══════════════════════════════════════════════════════════════════════════
# Keep Application Class
# ═══════════════════════════════════════════════════════════════════════════
-keep class com.aktarjabed.inbusiness.InBusinessApp { *; }