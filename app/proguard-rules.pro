# Room
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# Hilt
-keep class * extends androidx.hilt.lifecycle.HiltViewModelFactory { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# iText (PDF)
-keep class com.itextpdf.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# PDF Viewer
-keep class com.github.barteksc.pdfviewer.** { *; }

# Kotlinx-datetime
-keep class kotlinx.datetime.** { *; }