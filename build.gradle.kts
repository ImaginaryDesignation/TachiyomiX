buildscript {
    dependencies {
        classpath(libs.android.shortcut.gradle)
        classpath(libs.google.services.gradle)
        classpath(libs.aboutlibraries.gradle)
        classpath(kotlinx.serialization.gradle)
    }
}

plugins {
    // TX-->
    id("com.android.application") version "7.1.3" apply false
    id("com.android.library") version "7.1.3" apply false
    kotlin("android") version "1.6.10" apply false
    id("org.jmailen.kotlinter") version "3.6.0"
    id("com.github.ben-manes.versions") version "0.40.0"
    // TX<--

    /*alias(androidx.plugins.application) apply false
    alias(androidx.plugins.library) apply false
    alias(kotlinx.plugins.android) apply false
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.versionsx)*/
}

subprojects {
    apply<org.jmailen.gradle.kotlinter.KotlinterPlugin>()

    kotlinter {
        experimentalRules = true

        // Doesn't play well with Android Studio
        disabledRules = arrayOf("experimental:argument-list-wrapping")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
