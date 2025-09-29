# Migration Guide from Java Concurrent Collections

This guide helps you migrate from Java's standard concurrent collections to TreiberStack, providing equivalent patterns and highlighting important differences.

## Table of Contents

1. [Migration Overview](#migration-overview)
2. [From ConcurrentLinkedDeque](#from-concurrentlinkeddeque)
3. [From ConcurrentLinkedQueue](#from-concurrentlinkedqueue)
4. [From Collections.synchronizedList](#from-collectionssynchronizedlist)
5. [From BlockingDeque/BlockingQueue](#from-blockingdequeblockingqueue)
6. [Performance Migration Considerations](#performance-migration-considerations)
7. [Common Migration Patterns](#common-migration-patterns)
8. [Code Examples](#code-examples)
9. [Migration Checklist](#migration-checklist)
10. [Troubleshooting](#troubleshooting)

## Migration Overview

### Why Migrate to TreiberStack?

**Benefits:**
- **Lock-free performance**: No blocking operations, better scalability
- **Predictable latency**: No lock contention delays
- **Coroutine-friendly**: Native suspend function support
- **ABA-safe**: Built-in protection against ABA problems
- **Memory efficient**: Lower memory overhead in high-contention scenarios

**Consider staying with Java collections if:**
- Single-threaded or low-concurrency use cases
- Need queue semantics (FIFO) rather than stack (LIFO)
- Ultra-high performance single-threaded access required
- Existing code works well without contention issues

### Key Differences to Understand

| Aspect | Java Collections | TreiberStack |
|--------|------------------|--------------|
| Access Pattern | FIFO (Queue) or both ends (Deque) | LIFO (Stack) only |
| Blocking | Some operations may block | Never blocks |
| Exception Model | Exceptions for invalid states | Returns null for empty |
| Coroutine Support | Blocking calls | Native suspend functions |
| Memory Model | JVM optimized, mature | Lock-free, versioned references |

## From ConcurrentLinkedDeque

### When Used as a Stack (addFirst/removeFirst)

ConcurrentLinkedDeque used in stack mode is the closest match to TreiberStack.

#### Before (ConcurrentLinkedDeque)
```java
import java.util.concurrent.ConcurrentLinkedDeque;

public class JavaStackExample {
    private final ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();
    
    // Push to stack
    public void push(String item) {
        deque.addFirst(item);
    }
    
    // Pop from stack  
    public String pop() {
        return deque.removeFirst(); // throws NoSuchElementException if empty
    }
    
    // Peek at top
    public String peek() {
        return deque.peekFirst(); // returns null if empty
    }
    
    // Check if empty
    public boolean isEmpty() {
        return deque.isEmpty();
    }
    
    // Get size
    public int size() {
        return deque.size();
    }
}
```

#### After (TreiberStack)
```kotlin
class KotlinStackExample {
    private val stack = TreiberStack<String>()
    
    // Push to stack
    suspend fun push(item: String) {
        stack.push(item)
    }
    
    // Pop from stack
    suspend fun pop(): String? {
        return stack.pop() // returns null if empty (no exception)
    }
    
    // Peek at top
    fun peek(): String? {
        return stack.peek()
    }
    
    // Check if empty
    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }
    
    // Get size
    fun size(): Int {
        return stack.size()
    }
}
```

#### Migration Steps

1. **Replace exception handling**:
```java
// Before (Java)
try {
    String item = deque.removeFirst();
    process(item);
} catch (NoSuchElementException e) {
    // Handle empty deque
}
```

```kotlin
// After (Kotlin)
val item = stack.pop()
if (item != null) {
    process(item)
} else {
    // Handle empty stack
}
```

2. **Add suspend functions**:
```kotlin
// Before (Java - blocking)
public void producerConsumer() {
    // Producer
    new Thread(() -> {
        for (int i = 0; i < 1000; i++) {
            deque.addFirst("item-" + i);
        }
    }).start();
    
    // Consumer
    new Thread(() -> {
        while (true) {
            try {
                String item = deque.removeFirst();
                process(item);
            } catch (NoSuchElementException e) {
                // Empty, continue
            }
        }
    }).start();
}

// After (Kotlin - coroutines)
suspend fun producerConsumer() = coroutineScope {
    // Producer
    launch {
        repeat(1000) { i ->
            stack.push("item-$i")
        }
    }
    
    // Consumer  
    launch {
        while (true) {
            val item = stack.pop()
            if (item != null) {
                process(item)
            } else {
                yield() // Cooperative scheduling
            }
        }
    }
}
```

### When Used as Double-Ended Queue

TreiberStack only supports stack operations (single end). If you need double-ended functionality:

#### Before (ConcurrentLinkedDeque)
```java
deque.addFirst(item);   // Push to front
deque.addLast(item);    // Push to back  
deque.removeFirst();    // Pop from front
deque.removeLast();     // Pop from back
```

#### Migration Options
```kotlin
// Option 1: Two TreiberStacks (if access patterns allow)
class DoubleEndedAdapter<T> {
    private val frontStack = TreiberStack<T>()
    private val backStack = TreiberStack<T>()
    
    suspend fun addFirst(item: T) = frontStack.push(item)
    suspend fun addLast(item: T) = backStack.push(item)
    suspend fun removeFirst(): T? = frontStack.pop()
    suspend fun removeLast(): T? = backStack.pop()
}

// Option 2: Keep ConcurrentLinkedDeque for double-ended access
// Option 3: Use different data structure (Channel, etc.)
```

## From ConcurrentLinkedQueue

ConcurrentLinkedQueue provides FIFO semantics, while TreiberStack provides LIFO. Direct migration requires semantic changes.

#### Before (ConcurrentLinkedQueue)
```java
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueExample {
    private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
    
    // Add to tail
    public void enqueue(Task task) {
        queue.offer(task);
    }
    
    // Remove from head (FIFO)
    public Task dequeue() {
        return queue.poll(); // returns null if empty
    }
}
```

#### Migration Considerations

1. **Semantic Change**: FIFO → LIFO
```kotlin
// If FIFO semantics are required, consider:
class FIFOSimulation<T> {
    private val stack = TreiberStack<T>()
    private val tempStack = TreiberStack<T>()
    
    suspend fun enqueue(item: T) {
        stack.push(item)
    }
    
    suspend fun dequeue(): T? {
        // Move all items to temp stack (reverses order)
        while (!stack.isEmpty()) {
            val item = stack.pop()
            if (item != null) tempStack.push(item)
        }
        
        // Take first item (original FIFO order)
        val result = tempStack.pop()
        
        // Move remaining items back
        while (!tempStack.isEmpty()) {
            val item = tempStack.pop()
            if (item != null) stack.push(item)
        }
        
        return result
    }
}

// Better: Use Channel for FIFO semantics
val channel = Channel<Task>(capacity = Channel.UNLIMITED)
// send() and receive() provide FIFO with suspend support
```

2. **Keep Queue if FIFO is Essential**:
```kotlin
// Sometimes the right answer is not to migrate
class HybridApproach {
    private val queue = ConcurrentLinkedQueue<Task>() // Keep for FIFO
    private val stack = TreiberStack<Result>()        // Use for LIFO results
    
    suspend fun processWorkflow() {
        val task = queue.poll() // FIFO task processing
        if (task != null) {
            val result = processTask(task)
            stack.push(result) // LIFO result handling
        }
    }
}
```

## From Collections.synchronizedList

Traditional synchronized collections have different characteristics.

#### Before (Collections.synchronizedList)
```java
import java.util.*;

public class SynchronizedExample {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());
    
    public void addItem(String item) {
        list.add(item); // Synchronized
    }
    
    public String getLastItem() {
        synchronized(list) {
            if (!list.isEmpty()) {
                return list.remove(list.size() - 1);
            }
            return null;
        }
    }
    
    public int getSize() {
        return list.size(); // Synchronized
    }
}
```

#### After (TreiberStack)
```kotlin
class LockFreeExample {
    private val stack = TreiberStack<String>()
    
    suspend fun addItem(item: String) {
        stack.push(item) // Lock-free
    }
    
    suspend fun getLastItem(): String? {
        return stack.pop() // Lock-free, atomic
    }
    
    fun getSize(): Int {
        return stack.size() // Lock-free snapshot
    }
}
```

#### Key Migration Benefits
- **No external synchronization needed**: Operations are inherently thread-safe
- **Better performance**: No lock contention
- **Deadlock-free**: No possibility of deadlocks
- **Composable**: Operations can be combined with other suspend functions

## From BlockingDeque/BlockingQueue

Blocking collections provide flow control through blocking. TreiberStack is non-blocking.

#### Before (LinkedBlockingDeque)
```java
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class BlockingExample {
    private final LinkedBlockingDeque<WorkItem> deque = new LinkedBlockingDeque<>(1000);
    
    public void producer() {
        try {
            WorkItem item = createWork();
            deque.putFirst(item); // Blocks if full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void consumer() {
        try {
            WorkItem item = deque.takeFirst(); // Blocks if empty
            processWork(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### Migration with Flow Control
```kotlin
class NonBlockingExample {
    private val stack = TreiberStack<WorkItem>()
    private val maxSize = 1000
    
    suspend fun producer() {
        val item = createWork()
        
        // Implement backpressure manually
        while (stack.size() >= maxSize) {
            delay(1) // Cooperative waiting
        }
        
        stack.push(item)
    }
    
    suspend fun consumer() {
        while (true) {
            val item = stack.pop()
            if (item != null) {
                processWork(item)
            } else {
                delay(10) // Wait for new items
            }
        }
    }
}
```

#### Alternative: Use Channels for Blocking Behavior
```kotlin
class ChannelBasedExample {
    private val channel = Channel<WorkItem>(capacity = 1000)
    
    suspend fun producer() {
        val item = createWork()
        channel.send(item) // Suspends if full
    }
    
    suspend fun consumer() {
        val item = channel.receive() // Suspends if empty
        processWork(item)
    }
}
```

## Performance Migration Considerations

### Benchmarking Before Migration

```kotlin
class MigrationBenchmark {
    suspend fun comparePerformance() {
        val iterations = 100_000
        val threadCount = 8
        
        // Benchmark existing Java collection
        val javaTime = measureTime {
            val deque = ConcurrentLinkedDeque<Int>()
            runBlocking {
                coroutineScope {
                    repeat(threadCount) {
                        launch(Dispatchers.Default) {
                            repeat(iterations / threadCount) { i ->
                                deque.addFirst(i)
                                deque.removeFirst()
                            }
                        }
                    }
                }
            }
        }
        
        // Benchmark TreiberStack
        val treiberTime = measureTime {
            val stack = TreiberStack<Int>()
            runBlocking {
                coroutineScope {
                    repeat(threadCount) {
                        launch(Dispatchers.Default) {
                            repeat(iterations / threadCount) { i ->
                                stack.push(i)
                                stack.pop()
                            }
                        }
                    }
                }
            }
        }
        
        println("Java ConcurrentLinkedDeque: ${javaTime.inWholeMilliseconds}ms")
        println("TreiberStack: ${treiberTime.inWholeMilliseconds}ms")
        println("Speedup: ${javaTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds}")
    }
}
```

### Expected Performance Changes

| Scenario | Expected Change | Notes |
|----------|----------------|-------|
| Single-threaded | 3-6x slower | Atomic operation overhead |
| Low contention (2-4 threads) | Comparable | Similar performance |
| High contention (8+ threads) | 1.2-1.6x faster | Lock-free advantage |
| Memory pressure | Variable | Depends on allocation patterns |

## Common Migration Patterns

### Pattern 1: Task Queue Migration

#### Before (Java)
```java
public class TaskProcessor {
    private final ConcurrentLinkedDeque<Runnable> taskQueue = new ConcurrentLinkedDeque<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public void submitTask(Runnable task) {
        taskQueue.addLast(task);
    }
    
    public void processAllTasks() {
        executor.submit(() -> {
            while (!taskQueue.isEmpty()) {
                Runnable task = taskQueue.removeFirst();
                if (task != null) {
                    task.run();
                }
            }
        });
    }
}
```

#### After (Kotlin)
```kotlin
class TaskProcessor {
    private val taskQueue = TreiberStack<suspend () -> Unit>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun submitTask(task: suspend () -> Unit) {
        taskQueue.push(task)
    }
    
    fun processAllTasks() {
        scope.launch {
            while (!taskQueue.isEmpty()) {
                val task = taskQueue.pop()
                if (task != null) {
                    task()
                }
                yield() // Cooperative scheduling
            }
        }
    }
    
    fun shutdown() {
        scope.cancel()
    }
}
```

### Pattern 2: Producer-Consumer Migration

#### Before (Java)
```java
public class ProducerConsumer {
    private final ConcurrentLinkedDeque<Data> buffer = new ConcurrentLinkedDeque<>();
    private volatile boolean running = true;
    
    public void startProducer() {
        new Thread(() -> {
            while (running) {
                Data data = generateData();
                buffer.addFirst(data);
                try { Thread.sleep(10); } catch (InterruptedException e) { break; }
            }
        }).start();
    }
    
    public void startConsumer() {
        new Thread(() -> {
            while (running) {
                Data data = buffer.removeFirst();
                if (data != null) {
                    processData(data);
                } else {
                    try { Thread.sleep(5); } catch (InterruptedException e) { break; }
                }
            }
        }).start();
    }
}
```

#### After (Kotlin)
```kotlin
class ProducerConsumer {
    private val buffer = TreiberStack<Data>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun start() {
        // Producer
        scope.launch {
            while (isActive) {
                val data = generateData()
                buffer.push(data)
                delay(10)
            }
        }
        
        // Consumer
        scope.launch {
            while (isActive) {
                val data = buffer.pop()
                if (data != null) {
                    processData(data)
                } else {
                    delay(5)
                }
            }
        }
    }
    
    fun stop() {
        scope.cancel()
    }
}
```

### Pattern 3: Event Buffer Migration

#### Before (Java)
```java
public class EventBuffer {
    private final ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<>();
    private final int maxSize = 10000;
    
    public void addEvent(Event event) {
        events.addFirst(event);
        
        // Trim old events
        while (events.size() > maxSize) {
            events.removeLast();
        }
    }
    
    public List<Event> getRecentEvents(int count) {
        List<Event> result = new ArrayList<>();
        Iterator<Event> it = events.iterator();
        for (int i = 0; i < count && it.hasNext(); i++) {
            result.add(it.next());
        }
        return result;
    }
}
```

#### After (Kotlin)
```kotlin
class EventBuffer {
    private val events = TreiberStack<Event>()
    private val maxSize = 10000
    private val trimLock = Mutex()
    
    suspend fun addEvent(event: Event) {
        events.push(event)
        
        // Trim old events (with synchronization to avoid race conditions)
        trimLock.withLock {
            val overflow = events.size() - maxSize
            if (overflow > 0) {
                // Remove excess events from bottom (oldest first)
                val temp = mutableListOf<Event>()
                
                // Pop all events
                while (!events.isEmpty()) {
                    val e = events.pop()
                    if (e != null) temp.add(e)
                }
                
                // Push back only the most recent maxSize events
                temp.take(maxSize).reversed().forEach { events.push(it) }
            }
        }
    }
    
    suspend fun getRecentEvents(count: Int): List<Event> {
        val result = mutableListOf<Event>()
        val temp = TreiberStack<Event>()
        
        // Pop up to 'count' events
        repeat(count) {
            val event = events.pop()
            if (event != null) {
                result.add(event)
                temp.push(event)
            }
        }
        
        // Restore events to original stack
        while (!temp.isEmpty()) {
            val event = temp.pop()
            if (event != null) events.push(event)
        }
        
        return result
    }
}
```

## Code Examples

### Complete Migration Example

#### Java Original
```java
import java.util.concurrent.*;

public class WorkStealing {
    private final ConcurrentLinkedDeque<Task> globalQueue = new ConcurrentLinkedDeque<>();
    private final ExecutorService workers;
    private volatile boolean shutdown = false;
    
    public WorkStealing(int workerCount) {
        this.workers = Executors.newFixedThreadPool(workerCount);
        
        // Start workers
        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            workers.submit(() -> {
                while (!shutdown) {
                    Task task = globalQueue.pollFirst();
                    if (task != null) {
                        task.execute();
                    } else {
                        try { Thread.sleep(1); } catch (InterruptedException e) { break; }
                    }
                }
            });
        }
    }
    
    public void submitTask(Task task) {
        globalQueue.addFirst(task);
    }
    
    public void shutdown() {
        shutdown = true;
        workers.shutdown();
    }
}
```

#### Kotlin Migration
```kotlin
import kotlinx.coroutines.*

class WorkStealing(private val workerCount: Int) {
    private val globalQueue = TreiberStack<Task>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        startWorkers()
    }
    
    private fun startWorkers() {
        repeat(workerCount) { workerId ->
            scope.launch {
                while (isActive) {
                    val task = globalQueue.pop()
                    if (task != null) {
                        task.execute()
                    } else {
                        delay(1) // Cooperative waiting
                    }
                }
            }
        }
    }
    
    suspend fun submitTask(task: Task) {
        globalQueue.push(task)
    }
    
    fun shutdown() {
        scope.cancel()
    }
}
```

## Migration Checklist

### Pre-Migration Assessment
- [ ] **Identify access patterns**: LIFO vs FIFO vs both ends
- [ ] **Measure current performance**: Baseline metrics
- [ ] **Analyze contention levels**: Thread count and usage patterns  
- [ ] **Review error handling**: Exception vs null return patterns
- [ ] **Assess integration points**: Blocking vs non-blocking requirements

### Code Migration Steps
- [ ] **Replace collection imports**: Java collections → TreiberStack
- [ ] **Convert to suspend functions**: Add suspend to push/pop operations
- [ ] **Update exception handling**: try/catch → null checks
- [ ] **Migrate thread management**: Thread/ExecutorService → Coroutines
- [ ] **Add cooperative scheduling**: yield() calls in loops
- [ ] **Update synchronization**: Remove external locks

### Post-Migration Validation
- [ ] **Functional testing**: All operations work correctly
- [ ] **Performance testing**: Compare with baseline metrics
- [ ] **Stress testing**: High-contention scenarios
- [ ] **Memory profiling**: Check for leaks or excessive allocation
- [ ] **Integration testing**: Verify with dependent systems

### Rollback Plan
- [ ] **Feature flags**: Ability to switch back to old implementation
- [ ] **Monitoring**: Metrics to detect performance regressions
- [ ] **Documentation**: Record migration decisions and trade-offs

## Troubleshooting

### Common Issues and Solutions

#### Issue: Performance Regression in Single-Threaded Use
```kotlin
// Problem: TreiberStack slower than ArrayList for single-threaded
// Solution: Consider keeping simple collections for single-threaded use

class AdaptiveDataStructure<T> {
    private val singleThreaded: MutableList<T> = mutableListOf()
    private val multiThreaded: TreiberStack<T> = TreiberStack()
    private val threadCount = AtomicInteger(0)
    
    suspend fun push(item: T) {
        if (threadCount.incrementAndGet() == 1) {
            // Single threaded - use simple list
            singleThreaded.add(item)
            threadCount.decrementAndGet()
        } else {
            threadCount.decrementAndGet()
            multiThreaded.push(item)
        }
    }
}
```

#### Issue: Missing FIFO Semantics
```kotlin
// Problem: Need FIFO but TreiberStack is LIFO
// Solution: Use Channel or implement FIFO adapter

suspend fun fifoAlternative() {
    // Option 1: Use Channel
    val channel = Channel<String>(Channel.UNLIMITED)
    channel.send("item")
    val item = channel.receive()
    
    // Option 2: Implement FIFO adapter (expensive)
    class FIFOAdapter<T> {
        private val stack = TreiberStack<T>()
        
        suspend fun enqueue(item: T) {
            val temp = TreiberStack<T>()
            
            // Move all items to temp
            while (!stack.isEmpty()) {
                val existing = stack.pop()
                if (existing != null) temp.push(existing)
            }
            
            // Add new item
            stack.push(item)
            
            // Restore items
            while (!temp.isEmpty()) {
                val existing = temp.pop()
                if (existing != null) stack.push(existing)
            }
        }
    }
}
```

#### Issue: Blocking Semantics Required
```kotlin
// Problem: Need blocking behavior for flow control
// Solution: Implement backpressure or use channels

class BackpressureStack<T>(private val maxSize: Int) {
    private val stack = TreiberStack<T>()
    private val semaphore = Semaphore(maxSize)
    
    suspend fun push(item: T) {
        semaphore.acquire() // Block until space available
        stack.push(item)
    }
    
    suspend fun pop(): T? {
        val item = stack.pop()
        if (item != null) {
            semaphore.release() // Release space
        }
        return item
    }
}
```

#### Issue: Memory Leaks from Versioning
```kotlin
// Problem: VersionedReference objects accumulate
// Solution: This is usually not an issue (objects are small and short-lived)
// But monitor GC behavior

class MemoryMonitoredStack<T> {
    private val stack = TreiberStack<T>()
    private var lastGCCheck = System.currentTimeMillis()
    
    suspend fun push(item: T) {
        stack.push(item)
        
        // Periodically trigger GC if needed
        val now = System.currentTimeMillis()
        if (now - lastGCCheck > 30000) { // Every 30 seconds
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val threshold = runtime.totalMemory() * 0.8
            
            if (usedMemory > threshold) {
                System.gc() // Suggest GC
            }
            lastGCCheck = now
        }
    }
}
```

### Performance Debugging

```kotlin
class PerformanceDebugger<T> {
    private val stack = TreiberStack<T>()
    private val retryCount = AtomicLong(0)
    private val operationCount = AtomicLong(0)
    
    suspend fun debuggedPush(item: T) {
        var attempts = 0
        val start = System.nanoTime()
        
        try {
            stack.push(item)
            attempts = 1 // Assume minimal retries for successful push
        } finally {
            val duration = System.nanoTime() - start
            operationCount.incrementAndGet()
            
            if (attempts > 1) {
                retryCount.addAndGet(attempts - 1.toLong())
            }
            
            // Log slow operations
            if (duration > 1_000_000) { // > 1ms
                println("Slow push: ${duration / 1_000_000}ms, attempts: $attempts")
            }
        }
    }
    
    fun printStatistics() {
        val total = operationCount.get()
        val retries = retryCount.get()
        println("Operations: $total")
        println("Retries: $retries")
        println("Retry rate: ${retries.toDouble() / total * 100}%")
    }
}
```

## Summary

Migrating from Java concurrent collections to TreiberStack can provide significant performance benefits in high-contention scenarios while offering better integration with Kotlin coroutines. Key considerations:

1. **Semantic compatibility**: Ensure LIFO semantics meet your requirements
2. **Performance testing**: Always benchmark your specific use case
3. **Gradual migration**: Consider hybrid approaches during transition
4. **Monitoring**: Watch for performance regressions and memory usage

The lock-free nature of TreiberStack eliminates many concurrency issues while providing predictable performance characteristics, making it an excellent choice for modern concurrent applications.