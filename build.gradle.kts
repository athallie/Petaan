import org.jetbrains.kotlin.fir.declarations.builder.buildScript

// Top-level build file where you can add configuration options common to all sub-projects/modules.\

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(libs.secrets.gradle.plugin)
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.4")
    }
}