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

// Configure publishing for Maven Central
publishing {
    publications {
        // Publications will be created automatically by multiplatform plugin
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                else
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            )
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

// Configure signing
signing {
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

// Configure publication metadata
publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Treiber-Stack")
            description.set("Lock-free concurrent stack implementation using Treiber's algorithm for Kotlin Multiplatform")
            url.set("https://github.com/menon-codes/Treiber-Stack")
            
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            
            developers {
                developer {
                    id.set("menon-codes")
                    name.set("Your Name")
                    email.set("your.email@example.com")
                }
            }
            
            scm {
                connection.set("scm:git:git://github.com/menon-codes/Treiber-Stack.git")
                developerConnection.set("scm:git:ssh://github.com:menon-codes/Treiber-Stack.git")
                url.set("https://github.com/menon-codes/Treiber-Stack")
            }
        }
    }
}
