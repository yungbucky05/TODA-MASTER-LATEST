plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.toda"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.toda"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Add flavor dimension and product flavors for passenger/driver/admin
    flavorDimensions += "role"
    productFlavors {
        create("passenger") {
            dimension = "role"
            applicationIdSuffix = ".passenger"
            versionNameSuffix = "-passenger"
            resValue("string", "app_name", "TODA Passenger")
        }
        create("driver") {
            dimension = "role"
            applicationIdSuffix = ".driver"
            versionNameSuffix = "-driver"
            resValue("string", "app_name", "TODA Driver")
        }
        create("admin") {
            dimension = "role"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"
            resValue("string", "app_name", "TODA Admin")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Enable detailed logging in debug builds
            buildConfigField("boolean", "DEBUG_MODE", "true")
            // Add OpenRouteService API key
            buildConfigField("String", "OPENROUTE_API_KEY", "\"eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjA0ZjA4NzM2ZjA0NzQ5NmU5YTA1M2NkYThiZmI0OWI3IiwiaCI6Im11cm11cjY0In0=\"")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign with debug key for now - this will work on all devices
            signingConfig = signingConfigs.getByName("debug")
            // Disable detailed logging in release builds
            buildConfigField("boolean", "DEBUG_MODE", "false")
            // Add OpenRouteService API key for release
            buildConfigField("String", "OPENROUTE_API_KEY", "\"eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjA0ZjA4NzM2ZjA0NzQ5NmU5YTA1M2NkYThiZmI0OWI3IiwiaCI6Im11cm11cjY0In0=\"")
            // Ensure release builds are more stable
            isJniDebuggable = false
            isRenderscriptDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Map and location dependencies
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("androidx.compose.ui:ui-viewbinding:1.5.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-functions-ktx") // Firebase Functions for callable cloud functions

    // Google Play Services dependencies for authentication
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")

    // Hilt dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Routing dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // JSON processing for Firebase sync
    implementation("com.google.code.gson:gson:2.10.1")

    // Additional dependencies for authentication
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
}
