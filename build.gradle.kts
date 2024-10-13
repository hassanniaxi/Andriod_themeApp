// File: build.gradle.kts (Project-level)
plugins {
    // Apply the plugins for configuration purposes. These will be applied in the app-level build.gradle.kts file.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}