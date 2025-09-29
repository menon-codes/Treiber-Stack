# Treiber-Stack

A lock-free concurrent stack implementation using Treiber's algorithm for Kotlin/JVM.

## Overview

This project implements a thread-safe, lock-free stack data structure based on R. Kent Treiber's algorithm. The implementation uses Compare-And-Swap (CAS) operations to achieve thread safety without traditional locking mechanisms, providing excellent performance in highly concurrent scenarios on the JVM.

## Features

- **Lock-free**: Uses atomic CAS operations instead of locks
- **JVM-optimized**: Leverages `java.util.concurrent.atomic.AtomicReference`
- **Coroutine-friendly**: Integrates seamlessly with Kotlin coroutines
- **Memory-safe**: Implements proper memory reclamation strategies
- **High-performance**: Optimized for concurrent access patterns

## Usage

```kotlin
// Basic usage
val stack = TreiberStack<String>()

// Push elements
stack.push("first")
stack.push("second")
stack.push("third")

// Pop elements (LIFO order)
val item = stack.pop() // Returns "third"

// Coroutine-friendly usage
suspend fun example() {
    val stack = TreiberStack<Int>()

    // Concurrent operations
    launch { repeat(1000) { stack.push(it) } }
    launch { repeat(1000) { stack.pop() } }
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.menon-codes:treiber-stack:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.github.menon-codes:treiber-stack:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.menon-codes</groupId>
    <artifactId>treiber-stack</artifactId>
    <version>1.0.0</version>
</dependency>
```
