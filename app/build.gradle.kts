import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.kelompok2.petaan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kelompok2.petaan"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            type = "String",
            name = "API_KEY",
            value = localProperties["API_KEY"].toString()
        )

        buildConfigField(
            type = "String",
            name = "TS_ADMIN_API_KEY",
            value = localProperties["TS_ADMIN_API_KEY"].toString()
        )

        buildConfigField(
            type = "String",
            name = "TS_SEARCH_API_KEY",
            value = localProperties["TS_SEARCH_API_KEY"].toString()
        )
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.airbnb.android:lottie:6.6.0")
    implementation("io.appwrite:sdk-for-android:6.0.0")
    implementation("io.coil-kt.coil3:coil:3.0.2")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.2")
    implementation("com.algolia:algoliasearch-client-kotlin:3.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation (libs.play.services.location)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.sdk)
    implementation(libs.androidx.navigation.compose)
    implementation (libs.androidx.navigation.fragment)
    implementation (libs.androidx.navigation.ui)
    implementation (libs.androidx.navigation.dynamic.features.fragment)
    androidTestImplementation (libs.androidx.navigation.testing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}