plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.campushelper.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.campushelper.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Room schema export (Phase 3.1): every schema version is written under
// /schemas as JSON (see app/schemas/.../2.json) so migrations can be reviewed
// and future migrations tested with MigrationTestHelper.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Bundle the exported Room schemas as androidTest assets so MigrationTestHelper
// can recreate a previous schema version during migration tests. The source
// files are named "<version>.json"; they are renamed to "<Database>_<version>.json"
// to match MigrationTestHelper's lookup convention.
val copyRoomSchemas by tasks.registering(Copy::class) {
    // MigrationTestHelper looks for schema JSONs under
    // assets/<fully.qualified.DatabaseClass>/<version>.json, which mirrors the
    // KSP output layout under app/schemas/, so copy the whole tree verbatim.
    from("$projectDir/schemas")
    into("$projectDir/src/androidTest/assets")
    include("**/*.json")
}
tasks.named("preBuild") { dependsOn(copyRoomSchemas) }

dependencies {
    // Compose: UI artifact versions come from the Compose BOM alignment
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // AndroidX core + Lifecycle (ViewModel, state collection from Compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose (type-safe navigation routes)
    implementation(libs.androidx.navigation.compose)

    // Kotlin coroutines (MVVM + Room + Retrofit all use them)
    implementation(libs.kotlinx.coroutines.android)

    // Room database. The compiler is applied through KSP (no KAPT).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking + Kotlin serialization (weather API comes in a later phase)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
