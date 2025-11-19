# Keep Room entities
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# Keep Hilt
-keep class * extends androidx.hilt.lifecycle.HiltViewModelFactory { *; }

# Keep Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Keep Generic signatures
-keepattributes Signature
-keepattributes *Annotation*