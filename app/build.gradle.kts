plugins {
    id("com.android.application") // Android application plugin
    id("org.jetbrains.kotlin.android") // Kotlin Android plugin
    id("com.google.gms.google-services") // Google Services plugin
    id("kotlin-kapt") // Kapt plugin for annotation processing
}


android {
    namespace = "com.example.taximeter" // Define namespace
    compileSdk = 35 // Ensure compileSdk matches the target

    defaultConfig {
        applicationId = "com.example.taximeter" // Unique application ID
        minSdk = 24 // Minimum supported SDK version
        targetSdk = 35 // Target SDK version
        versionCode = 1 // Version code for the app
        versionName = "1.0" // Version name for the app

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Instrumentation test runner
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Disable code shrinking in release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), // Default ProGuard rules
                "proguard-rules.pro" // Custom ProGuard rules
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Java 8 compatibility
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8" // Kotlin JVM target
    }
}

dependencies {
    implementation(libs.androidx.core.ktx) // Core Android extensions
    implementation(libs.androidx.appcompat) // AppCompat library for backward compatibility
    implementation(libs.material) // Material design components
    implementation(libs.androidx.activity) // Activity library
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore.ktx) // ConstraintLayout library

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Maps and Location Services
    implementation("com.google.android.gms:play-services-maps:18.1.0") // Google Maps API
    implementation("com.google.android.gms:play-services-location:21.0.1") // Location services

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Firebase BOM for version alignment
    implementation("com.google.firebase:firebase-analytics") // Firebase Analytics
    implementation("com.google.firebase:firebase-auth") // Firebase Authentication
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore-ktx:24.7.0") // Firestore
// Firebase Realtime Database

    // Lifecycle dependencies (with explicit versions)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1") // Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1") // LiveData
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // Lifecycle runtime

    // Room database
    implementation("androidx.room:room-runtime:2.5.0") // Room database runtime
    implementation("androidx.room:room-ktx:2.5.0") // Room Kotlin extensions
    implementation ("com.google.zxing:core:3.4.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation ("com.google.zxing:core:3.4.1")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("pub.devrel:easypermissions:3.0.0")


    // Room compiler for annotation processing
}
