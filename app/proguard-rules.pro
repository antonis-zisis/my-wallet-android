# Add project specific ProGuard rules here.

# Apollo Kotlin
-keep class com.apollographql.** { *; }
-keep class com.mywallet.android.graphql.** { *; }

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

# Hilt
-dontwarn dagger.hilt.**

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
