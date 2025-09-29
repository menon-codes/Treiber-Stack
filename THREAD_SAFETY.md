# Thread-Safety Guarantees

This document provides comprehensive information about the thread-safety guarantees of the TreiberStack implementation.

## Table of Contents

1. [Overview](#overview)
2. [Atomicity Guarantees](#atomicity-guarantees)
3. [Memory Consistency](#memory-consistency)
4. [ABA Problem Prevention](#aba-problem-prevention)
5. [Linearizability](#linearizability)
6. [Progress Guarantees](#progress-guarantees)
7. [Memory Model Compliance](#memory-model-compliance)
8. [Concurrent Operation Safety](#concurrent-operation-safety)
9. [Performance Characteristics](#performance-characteristics)
10. [Limitations and Considerations](#limitations-and-considerations)

## Overview

The `TreiberStack<T>` implementation provides **strong thread-safety guarantees** through lock-free algorithms based on Compare-And-Swap (CAS) operations. This document details the specific guarantees and their implications for concurrent usage.

## Atomicity Guarantees

### Core Operations

All fundamental stack operations are **atomically consistent**:

```kotlin
suspend fun push(item: T)    // Atomically adds item to top of stack
suspend fun pop(): T?        // Atomically removes and returns top item
fun peek(): T?               // Atomically reads top item without modification
fun isEmpty(): Boolean       // Atomically checks if stack is empty
fun size(): Int              // Provides consistent snapshot of stack size
```

### Atomicity Properties

1. **Push Operation**:
   - The item is either fully added to the stack or the operation fails and retries
   - No intermediate state where the stack is partially modified
   - Other threads see either the old state or the new state, never an inconsistent state

2. **Pop Operation**:
   - The item is either fully removed and returned, or null is returned for empty stack
   - No scenario where an item is removed but not returned, or returned but not removed
   - Stack remains consistent even if operation is interrupted

3. **Read Operations** (`peek()`, `isEmpty()`, `size()`):
   - Return consistent snapshots of the stack state at the time of the call
   - May become stale immediately after the call due to concurrent modifications

## Memory Consistency

### Happens-Before Relationships

The implementation establishes clear **happens-before** relationships:

```
Thread A: push(item)  ----happens-before----> Thread B: pop() returns item
Thread A: pop() returns null  ----happens-before----> Thread B: subsequent operations see empty stack
```

### Memory Ordering

All operations use **sequentially consistent** memory ordering through AtomicRef:

- **Write operations** (push/pop) are immediately visible to all other threads
- **Read operations** see the most recent committed state
- No reordering of atomic operations across threads

### Visibility Guarantees

```kotlin
// Example of visibility guarantee
class VisibilityExample {
    private val stack = TreiberStack<String>()
    
    // Thread A
    suspend fun producer() {
        stack.push("data")  // This write is immediately visible
    }
    
    // Thread B  
    suspend fun consumer() {
        val item = stack.pop()  // Will see the write from Thread A
        // if item != null, it contains the exact value written by Thread A
    }
}
```

## ABA Problem Prevention

### The ABA Problem

The ABA problem occurs when:
1. Thread A reads value A from a location
2. Thread B changes the value from A to B, then back to A
3. Thread A's CAS succeeds because the value is A, but the data structure may have changed

### Our Solution: Versioned References

```kotlin
internal data class VersionedReference<T>(val reference: T?, val version: Long)
```

**How it prevents ABA**:

1. Every modification increments the version number
2. CAS operations compare both reference AND version
3. Even if a node returns to the same position, its version will be different

### Example Scenario

```kotlin
// Initial state: head -> Node(A, version=0)

// Thread 1 reads: VersionedReference(Node(A), version=0)
// Thread 2: pop A, push B, push A
//   Result: VersionedReference(Node(A), version=3)
// Thread 1 attempts CAS with version=0 -> FAILS correctly
```

### Proof of ABA Prevention

The version number provides a **monotonically increasing witness** that ensures:
- If any modification occurred between read and CAS, the version will differ
- False positives are impossible (version only increases)
- Memory reclamation issues are avoided

## Linearizability

### Definition

Every operation appears to take effect **atomically** at some point between its invocation and response.

### Linearization Points

1. **Push Operation**: The successful CAS that updates the head reference
2. **Pop Operation**: The successful CAS that updates the head reference  
3. **Peek Operation**: The read of the head reference
4. **isEmpty/size**: The read of the head reference (for traversal start)

### Linearizability Proof Sketch

For any concurrent execution, we can identify linearization points such that:
- The sequential execution respecting these points produces the same results
- All operations appear atomic from external observers

```
Timeline:
T1: |--push(A)--[CAS success]--|
T2:    |--pop()--[CAS success]--| returns A
T3:       |--isEmpty()--[read]--| returns false

Linearization order: push(A), pop(), isEmpty()
```

## Progress Guarantees

### Lock-Freedom

The implementation guarantees **system-wide progress**:
- At least one thread makes progress in any finite number of steps
- No thread can prevent other threads from eventual progress
- No deadlocks, livelocks, or priority inversions possible

### Wait-Freedom Analysis

Individual operations are **not wait-free** but provide **bounded work**:
- Each retry iteration performs constant work
- Contention causes retries, but progress is guaranteed
- High contention may cause some threads to retry multiple times

### Starvation Freedom

**Weak fairness guarantee**:
- Under bounded contention, all threads make progress eventually
- No deterministic guarantee about maximum retry count
- Practical starvation is extremely rare due to `yield()` calls

## Memory Model Compliance

### JVM Memory Model

The implementation correctly handles:

1. **Volatile semantics**: All atomic operations provide volatile read/write semantics
2. **Constructor safety**: Properly constructed objects are safely published
3. **Final field semantics**: Node structure ensures safe publication

### Atomic Operations Mapping

```kotlin
AtomicRef<VersionedReference<Node<T>>>
├── compareAndSet() -> JVM CAS instruction
├── get() -> Volatile read
└── set() -> Volatile write (not used in our implementation)
```

### Memory Barriers

Implicit memory barriers in CAS operations ensure:
- **LoadLoad + LoadStore** barriers before the load
- **StoreStore + StoreLoad** barriers after the store
- **Full fence** semantics for failed CAS operations

## Concurrent Operation Safety

### Safe Operation Combinations

All operations are safe to call concurrently:

```kotlin
// All of these can safely run concurrently on the same stack
coroutineScope {
    launch { repeat(1000) { stack.push("item") } }      // Safe
    launch { repeat(1000) { stack.pop() } }             // Safe  
    launch { repeat(1000) { println(stack.peek()) } }   // Safe
    launch { repeat(1000) { println(stack.size()) } }   // Safe
    launch { repeat(1000) { println(stack.isEmpty()) } } // Safe
}
```

### Cross-Thread Visibility

Operations from any thread are immediately visible to all other threads:

```kotlin
// Thread safety demonstration
class ThreadSafetyDemo {
    private val stack = TreiberStack<Int>()
    
    // Safe: Multiple producers
    suspend fun producer1() { repeat(1000) { stack.push(it) } }
    suspend fun producer2() { repeat(1000) { stack.push(it + 1000) } }
    
    // Safe: Multiple consumers  
    suspend fun consumer1() { while(true) stack.pop() ?: break }
    suspend fun consumer2() { while(true) stack.pop() ?: break }
    
    // Safe: Mixed operations
    suspend fun mixedOperations() {
        stack.push(42)
        val size1 = stack.size()
        val item = stack.pop() 
        val size2 = stack.size()
        // size2 == size1 - 1 (unless other threads interfered)
    }
}
```

## Performance Characteristics

### Contention Behavior

1. **Low Contention**: Near-optimal performance, minimal retries
2. **Medium Contention**: Some retries, but bounded overhead
3. **High Contention**: More retries, but system-wide progress guaranteed

### Scalability Properties

```
Performance vs Thread Count:
- 1 thread:  Baseline performance (some atomic operation overhead)
- 2-4 threads: Linear scalability in most workloads  
- 8+ threads: Performance plateau due to memory system limits
- Very high contention: Graceful degradation, no collapse
```

### Memory Overhead

- **Per-stack overhead**: One AtomicRef + VersionedReference (≈24 bytes)
- **Per-node overhead**: Reference + item + version tracking (≈16 bytes + item size)
- **No hidden locks**: No synchronization objects or monitor overhead

## Limitations and Considerations

### Consistency Model Limitations

1. **Size Inconsistency**: `size()` may return stale values in concurrent scenarios
2. **Snapshot Isolation**: Read operations provide point-in-time snapshots, not transactional consistency
3. **No Ordering Guarantees**: Between different stacks or with external operations

### Performance Considerations

1. **Memory Overhead**: Versioning adds 8 bytes per reference update
2. **Contention Sensitivity**: Performance degrades under extreme contention
3. **Cache Coherence**: False sharing possible with multiple stacks

### Usage Guidelines

#### Do:
- Use for high-contention producer-consumer scenarios
- Combine with coroutine `yield()` for cooperative behavior
- Monitor memory usage in high-throughput scenarios

#### Don't:
- Rely on `size()` for critical business logic in concurrent scenarios
- Assume transactional behavior across multiple operations
- Use without `yield()` in tight retry loops

### Example of Limitation

```kotlin
// This is NOT atomic as a whole operation
suspend fun problematicPattern(stack: TreiberStack<String>) {
    if (!stack.isEmpty()) {  // Check
        val item = stack.pop()  // Action - may return null!
        // Another thread might have popped the last item between check and action
    }
}

// Correct pattern
suspend fun correctPattern(stack: TreiberStack<String>) {
    val item = stack.pop()  // Just try to pop
    if (item != null) {     // Check the result
        // Process item
    }
}
```

## Formal Guarantees Summary

1. **Safety**: No data races, no corrupted internal state
2. **Liveness**: System-wide progress under all conditions  
3. **Linearizability**: All operations appear atomic
4. **Sequential Consistency**: Operations appear in program order
5. **ABA Prevention**: Versioning eliminates ABA problems
6. **Memory Safety**: No use-after-free or double-free issues

These guarantees make `TreiberStack<T>` suitable for mission-critical concurrent applications where correctness and reliability are paramount.