buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24") // Kotlin Gradle Plugin
        classpath("com.android.tools.build:gradle:8.11.1") // Android Gradle Plugin
        classpath("com.google.gms:google-services:4.4.2") // Google Services plugin
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.24-1.0.20") // KSP
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
