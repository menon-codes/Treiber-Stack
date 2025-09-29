/*
 * Treiber-Stack Kotlin Multiplatform Library
 * Lock-free concurrent stack implementation
 */

/*
 * Treiber-Stack Kotlin JVM Library
 * Lock-free concurrent stack implementation
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            // Override the default artifactId
            artifactId = "treiber-stack"
            
            pom {
                name.set("Treiber-Stack")
                description.set("A lock-free concurrent stack implementation using Treiber's algorithm for Kotlin/JVM")
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
                        name.set("Aditya Menon")
                        email.set("ad.menon.dev@gmail.com")
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
            name = "ossrh-staging-api"
            val releasesRepoUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            url = releasesRepoUrl
            credentials {
                username = project.findProperty("sonatypeUsername") as String? ?: ""
                password = project.findProperty("sonatypePassword") as String? ?: ""
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
