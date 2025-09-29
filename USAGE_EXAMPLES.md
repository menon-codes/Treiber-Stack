# TreiberStack Usage Examples

This document provides comprehensive examples of how to use the `TreiberStack<T>` implementation in various scenarios.

## Table of Contents

1. [Basic Operations](#basic-operations)
2. [Coroutine Integration](#coroutine-integration)
3. [Producer-Consumer Patterns](#producer-consumer-patterns)
4. [Error Handling](#error-handling)
5. [Performance Considerations](#performance-considerations)
6. [Advanced Usage Patterns](#advanced-usage-patterns)

## Basic Operations

### Simple Push and Pop Operations

```kotlin
import io.github.menoncodes.treiberstack.TreiberStack
import kotlinx.coroutines.runBlocking

fun basicExample() = runBlocking {
    val stack = TreiberStack<String>()
    
    // Push elements onto the stack
    stack.push("first")
    stack.push("second") 
    stack.push("third")
    
    // Pop elements (LIFO - Last In, First Out)
    println(stack.pop()) // Prints: "third"
    println(stack.pop()) // Prints: "second"
    println(stack.pop()) // Prints: "first"
    println(stack.pop()) // Prints: null (empty stack)
}
```

### Checking Stack State

```kotlin
fun stackStateExample() = runBlocking {
    val stack = TreiberStack<Int>()
    
    println("Is empty: ${stack.isEmpty()}") // true
    println("Size: ${stack.size()}")        // 0
    println("Peek: ${stack.peek()}")        // null
    
    stack.push(42)
    stack.push(24)
    
    println("Is empty: ${stack.isEmpty()}") // false
    println("Size: ${stack.size()}")        // 2
    println("Peek: ${stack.peek()}")        // 24 (doesn't remove)
    println("Size after peek: ${stack.size()}") // still 2
}
```

## Coroutine Integration

### Using with Structured Concurrency

```kotlin
import kotlinx.coroutines.*

fun structuredConcurrencyExample() = runBlocking {
    val stack = TreiberStack<String>()
    
    coroutineScope {
        // Launch multiple concurrent operations
        val jobs = List(5) { index ->
            launch {
                stack.push("Item-$index")
                delay(10) // Simulate some work
                val item = stack.pop()
                println("Coroutine $index popped: $item")
            }
        }
        
        // All jobs will complete before this scope ends
    }
    
    println("Final stack size: ${stack.size()}")
}
```

### Integration with Flow

```kotlin
import kotlinx.coroutines.flow.*

fun flowIntegrationExample() = runBlocking {
    val stack = TreiberStack<Int>()
    
    // Producer flow
    val numbers = flow {
        repeat(10) { 
            emit(it)
            delay(50)
        }
    }
    
    // Consume numbers and push to stack
    numbers
        .onEach { number ->
            stack.push(number)
            println("Pushed: $number")
        }
        .collect()
    
    // Pop all elements
    while (!stack.isEmpty()) {
        println("Popped: ${stack.pop()}")
    }
}
```

## Producer-Consumer Patterns

### High Throughput Producer-Consumer

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.time.measureTime

fun producerConsumerExample() = runBlocking {
    val stack = TreiberStack<WorkItem>()
    val itemsToProcess = 10_000
    val producerCount = 4
    val consumerCount = 4
    
    data class WorkItem(val id: Int, val data: String)
    
    val time = measureTime {
        coroutineScope {
            // Start producers
            val producerJobs = List(producerCount) { producerId ->
                launch(Dispatchers.Default) {
                    repeat(itemsToProcess / producerCount) { itemId ->
                        val workItem = WorkItem(
                            id = producerId * 1000 + itemId,
                            data = "Data from producer $producerId"
                        )
                        stack.push(workItem)
                    }
                }
            }
            
            // Start consumers  
            val processedItems = Channel<WorkItem>(Channel.UNLIMITED)
            val consumerJobs = List(consumerCount) { consumerId ->
                launch(Dispatchers.Default) {
                    while (true) {
                        val item = stack.pop()
                        if (item != null) {
                            // Simulate processing
                            processedItems.send(item)
                        } else {
                            // Check if all producers are done
                            if (producerJobs.all { it.isCompleted } && stack.isEmpty()) {
                                break
                            }
                            yield() // Give other coroutines a chance
                        }
                    }
                }
            }
            
            // Wait for all producers to finish
            producerJobs.forEach { it.join() }
            
            // Wait for all consumers to finish
            consumerJobs.forEach { it.join() }
            processedItems.close()
            
            println("Processed ${processedItems.toList().size} items")
        }
    }
    
    println("Processing took: $time")
}
```

### Work Stealing Pattern

```kotlin
fun workStealingExample() = runBlocking {
    val globalWorkStack = TreiberStack<Task>()
    val workerCount = Runtime.getRuntime().availableProcessors()
    
    data class Task(val id: Int, val workload: suspend () -> String)
    
    // Populate with tasks
    repeat(100) { taskId ->
        val task = Task(taskId) {
            delay((10..100).random().toLong()) // Simulate variable work
            "Result from task $taskId"
        }
        globalWorkStack.push(task)
    }
    
    // Start workers
    coroutineScope {
        val workers = List(workerCount) { workerId ->
            launch(Dispatchers.Default) {
                var tasksProcessed = 0
                
                while (true) {
                    val task = globalWorkStack.pop()
                    if (task != null) {
                        val result = task.workload()
                        tasksProcessed++
                        println("Worker $workerId completed task ${task.id}: $result")
                    } else {
                        // No more work available
                        break
                    }
                }
                
                println("Worker $workerId processed $tasksProcessed tasks")
            }
        }
        
        workers.forEach { it.join() }
    }
    
    println("All work completed. Remaining tasks: ${globalWorkStack.size()}")
}
```

## Error Handling

### Graceful Degradation

```kotlin
import kotlin.coroutines.cancellation.CancellationException

fun errorHandlingExample() = runBlocking {
    val stack = TreiberStack<String>()
    
    try {
        coroutineScope {
            launch {
                repeat(1000) { index ->
                    if (index == 500) {
                        throw RuntimeException("Simulated error")
                    }
                    stack.push("Item-$index")
                }
            }
            
            launch {
                while (true) {
                    val item = stack.pop()
                    if (item != null) {
                        println("Processing: $item")
                        delay(1) // Simulate processing time
                    } else {
                        yield()
                    }
                }
            }
        }
    } catch (e: RuntimeException) {
        println("Error occurred: ${e.message}")
        println("Items remaining in stack: ${stack.size()}")
        
        // Drain remaining items for cleanup
        while (!stack.isEmpty()) {
            val item = stack.pop()
            println("Cleaning up: $item")
        }
    }
}
```

### Cancellation Handling

```kotlin
fun cancellationExample() = runBlocking {
    val stack = TreiberStack<Int>()
    
    val job = launch {
        try {
            repeat(Int.MAX_VALUE) { index ->
                stack.push(index)
                
                // Check for cancellation periodically
                if (index % 1000 == 0) {
                    yield() // Cooperative cancellation point
                }
            }
        } catch (e: CancellationException) {
            println("Operation was cancelled after pushing ${stack.size()} items")
            throw e // Re-throw to properly handle cancellation
        }
    }
    
    delay(100) // Let it run for a bit
    job.cancel("Taking too long")
    
    try {
        job.join()
    } catch (e: CancellationException) {
        println("Job was cancelled: ${e.message}")
    }
    
    println("Final stack size: ${stack.size()}")
}
```

## Performance Considerations

### Optimal Usage Patterns

```kotlin
fun performanceOptimalExample() = runBlocking {
    val stack = TreiberStack<ExpensiveObject>()
    
    data class ExpensiveObject(val id: Int, val data: ByteArray)
    
    // Good: Batch operations to reduce contention
    coroutineScope {
        // Producer batching
        launch(Dispatchers.Default) {
            val batch = mutableListOf<ExpensiveObject>()
            repeat(10_000) { id ->
                batch.add(ExpensiveObject(id, ByteArray(1024)))
                
                if (batch.size >= 100) {
                    // Push batch to reduce contention
                    batch.forEach { stack.push(it) }
                    batch.clear()
                }
            }
            // Push remaining items
            batch.forEach { stack.push(it) }
        }
        
        // Consumer with yielding to reduce contention
        launch(Dispatchers.Default) {
            var processed = 0
            while (processed < 10_000) {
                val item = stack.pop()
                if (item != null) {
                    // Process item
                    processed++
                } else {
                    // No items available, yield to reduce busy-waiting
                    yield()
                }
            }
            println("Processed $processed items")
        }
    }
}
```

### Avoiding Common Pitfalls

```kotlin
fun avoidPitfallsExample() = runBlocking {
    val stack = TreiberStack<String>()
    
    // Pitfall: Busy-waiting without yielding
    // BAD:
    /*
    while (stack.isEmpty()) {
        // Busy wait - wastes CPU
    }
    */
    
    // GOOD: Cooperative waiting
    while (stack.isEmpty()) {
        yield() // Allow other coroutines to run
        delay(1) // Small delay to reduce CPU usage
    }
    
    // Pitfall: Excessive size() calls
    // BAD:
    /*
    while (stack.size() > 0) {
        stack.pop() // size() is O(n)
    }
    */
    
    // GOOD: Use isEmpty() or peek()
    while (!stack.isEmpty()) {
        stack.pop()
    }
}
```

## Advanced Usage Patterns

### Stack as Event Buffer

```kotlin
fun eventBufferExample() = runBlocking {
    data class Event(val timestamp: Long, val type: String, val data: String)
    
    val eventBuffer = TreiberStack<Event>()
    val maxBufferSize = 1000
    
    // Event producer
    val eventProducer = launch(Dispatchers.Default) {
        var eventId = 0
        while (true) {
            val event = Event(
                timestamp = System.currentTimeMillis(),
                type = "UserAction",
                data = "Event data $eventId"
            )
            
            eventBuffer.push(event)
            eventId++
            
            // Prevent buffer overflow
            if (eventBuffer.size() > maxBufferSize) {
                // Remove old events (from bottom of stack)
                // Note: This is a simplified example
                val tempStack = TreiberStack<Event>()
                repeat(maxBufferSize / 2) {
                    val event = eventBuffer.pop()
                    if (event != null) tempStack.push(event)
                }
                // Clear original and restore half
                while (!eventBuffer.isEmpty()) eventBuffer.pop()
                while (!tempStack.isEmpty()) {
                    val event = tempStack.pop()
                    if (event != null) eventBuffer.push(event)
                }
            }
            
            delay(10) // Simulate event rate
        }
    }
    
    // Event processor
    val eventProcessor = launch(Dispatchers.Default) {
        while (true) {
            val event = eventBuffer.pop()
            if (event != null) {
                println("Processing event: ${event.type} at ${event.timestamp}")
            } else {
                delay(50) // No events to process
            }
        }
    }
    
    delay(5000) // Run for 5 seconds
    eventProducer.cancel()
    eventProcessor.cancel()
    
    println("Final buffer size: ${eventBuffer.size()}")
}
```

### Implementing Undo/Redo Functionality

```kotlin
fun undoRedoExample() = runBlocking {
    data class Command(val action: String, val undo: suspend () -> Unit)
    
    class UndoRedoManager {
        private val undoStack = TreiberStack<Command>()
        private val redoStack = TreiberStack<Command>()
        
        suspend fun executeCommand(command: Command) {
            println("Executing: ${command.action}")
            undoStack.push(command)
            
            // Clear redo stack when new command is executed
            while (!redoStack.isEmpty()) {
                redoStack.pop()
            }
        }
        
        suspend fun undo(): Boolean {
            val command = undoStack.pop()
            return if (command != null) {
                println("Undoing: ${command.action}")
                command.undo()
                redoStack.push(command)
                true
            } else {
                false
            }
        }
        
        suspend fun redo(): Boolean {
            val command = redoStack.pop()
            return if (command != null) {
                println("Redoing: ${command.action}")
                undoStack.push(command)
                true
            } else {
                false
            }
        }
        
        fun canUndo(): Boolean = !undoStack.isEmpty()
        fun canRedo(): Boolean = !redoStack.isEmpty()
    }
    
    val manager = UndoRedoManager()
    
    // Execute some commands
    manager.executeCommand(Command("Create file") { println("File deleted") })
    manager.executeCommand(Command("Edit content") { println("Content reverted") })
    manager.executeCommand(Command("Save file") { println("Save reverted") })
    
    // Undo operations
    manager.undo() // Undo save
    manager.undo() // Undo edit
    
    // Redo operation
    manager.redo() // Redo edit
    
    println("Can undo: ${manager.canUndo()}")
    println("Can redo: ${manager.canRedo()}")
}
```

## Best Practices Summary

1. **Use `yield()` in loops**: When waiting for conditions or in tight loops, always call `yield()` to enable cooperative multitasking.

2. **Batch operations when possible**: Group multiple push/pop operations to reduce contention in high-throughput scenarios.

3. **Prefer `isEmpty()` over `size()`**: The `size()` operation is O(n), while `isEmpty()` is O(1).

4. **Handle cancellation properly**: Always check for cancellation in long-running operations and re-throw `CancellationException`.

5. **Consider memory pressure**: In scenarios with large objects or high throughput, monitor memory usage and implement appropriate cleanup strategies.

6. **Profile your use case**: The lock-free nature shines in high-contention scenarios but may have overhead in low-contention cases.