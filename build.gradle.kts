// Top-level build file: declares the plugins used by the modules (no versions here,
// they are pinned once by the version catalog in gradle/libs.versions.toml).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Kotlin serialization: enables @Serializable types (used for type-safe navigation routes)
    alias(libs.plugins.kotlin.serialization) apply false
    // KSP: Kotlin Symbol Processing, runs the Room compiler (replaces KAPT)
    alias(libs.plugins.ksp) apply false
}
