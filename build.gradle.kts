/*
 * Root build file for Treiber-Stack Kotlin Multiplatform project
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "io.github.menon-codes"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
}

// Suppress warnings and info messages
gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        // Disable the problems report generation
        if (task.name.contains("problems")) {
            task.enabled = false
        }
    }
}