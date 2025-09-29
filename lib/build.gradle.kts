/*
 * Treiber-Stack Kotlin Multiplatform Library
 * Lock-free concurrent stack implementation
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.signing)
}

kotlin {
    // Configure multiplatform targets
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
                }
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }
    
    // Native targets
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.atomicfu)
            }
        }
        
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        jvmMain {
            dependencies {
                // JVM-specific dependencies if needed
            }
        }
        
        jvmTest {
            dependencies {
                // JVM-specific test dependencies
            }
        }
        
        jsMain {
            dependencies {
                // JS-specific dependencies if needed
            }
        }
        
        nativeMain {
            dependencies {
                // Native-specific dependencies if needed
            }
        }
    }
}
