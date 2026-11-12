# Proguard / R8 Optimization Rules for TapBot Release

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class **$$serializer {
    *;
}

# TapBot Models & Domain Entities
-keep class com.tapbot.core.model.** { *; }
-keep class com.tapbot.core.network.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Android Keystore & Security
-keep class com.tapbot.core.security.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep class com.tapbot.core.runner.** { *; }
-keep class com.tapbot.core.logging.** { *; }
-keep class com.tapbot.feature.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.Instant

# AndroidX Compose & Lifecycle
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
