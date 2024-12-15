buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // Kotlin Gradle Plugin
        classpath("com.android.tools.build:gradle:8.4.1") // Android Gradle Plugin
        classpath("com.google.gms:google-services:4.3.15") // Google Services plugin
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
