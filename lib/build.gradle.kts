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

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Treiber-Stack")
                description.set("A lock-free concurrent stack implementation using Treiber's algorithm for Kotlin Multiplatform")
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
                        name.set("Menon")
                        email.set("your-email@example.com") // Update with actual email
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/menon-codes/Treiber-Stack.git")
                    developerConnection.set("scm:git:ssh://github.com/menon-codes/Treiber-Stack.git")
                    url.set("https://github.com/menon-codes/Treiber-Stack")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

// Only sign if credentials are available
tasks.withType<Sign>().configureEach {
    enabled = project.hasProperty("signingKey") && project.hasProperty("signingPassword")
}
