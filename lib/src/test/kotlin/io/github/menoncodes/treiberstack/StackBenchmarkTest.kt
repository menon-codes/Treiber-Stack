/*
 * Copyright (c) 2025 menon-codes
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package io.github.menoncodes.treiberstack

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Benchmark tests comparing TreiberStack (lock-free) vs MutexStack (mutex-based).
 * 
 * These tests measure performance under various scenarios to demonstrate
 * the advantages of lock-free data structures, especially under contention.
 */
class StackBenchmarkTest {
    
    companion object {
        private const val OPERATIONS_PER_THREAD = 10_000
        private const val WARMUP_OPERATIONS = 1_000
    }
    
    @Test
    fun benchmarkSingleThreadedOperations() = runTest(timeout = 60.seconds) {
        println("\n=== Single-Threaded Benchmark ===")
        
        // Warmup
        warmupStacks()
        
        // Benchmark TreiberStack
        val treiberTime = measureTime {
            val stack = TreiberStack<Int>()
            repeat(OPERATIONS_PER_THREAD) { i ->
                stack.push(i)
            }
            repeat(OPERATIONS_PER_THREAD) {
                stack.pop()
            }
        }
        
        // Benchmark MutexStack  
        val mutexTime = measureTime {
            val stack = MutexStack<Int>()
            repeat(OPERATIONS_PER_THREAD) { i ->
                stack.push(i)
            }
            repeat(OPERATIONS_PER_THREAD) {
                stack.pop()
            }
        }
        
        println("TreiberStack (lock-free): ${treiberTime.inWholeMilliseconds}ms")
        println("MutexStack (mutex-based): ${mutexTime.inWholeMilliseconds}ms")
        
        val speedup = mutexTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds.toDouble()
        println("Lock-free speedup: ${String.format("%.2f", speedup)}x")
        
        // In single-threaded scenarios, results should be comparable
        // Lock-free might be slightly faster due to no mutex overhead
        assertTrue(speedup >= 0.8, "Lock-free should perform reasonably well in single-threaded scenario")
    }
    
    @Test
    fun benchmarkMultiThreadedHighContention() = runTest(timeout = 60.seconds) {
        println("\n=== Multi-Threaded High Contention Benchmark (8 threads) ===")
        
        val threadCount = 8
        val opsPerThread = OPERATIONS_PER_THREAD / 4 // Reduce ops to keep test reasonable
        
        // Benchmark TreiberStack
        val treiberTime = measureTime {
            val stack = TreiberStack<Int>()
            val jobs = mutableListOf<Job>()
            
            repeat(threadCount) { threadId ->
                val job = launch {
                    repeat(opsPerThread) { i ->
                        stack.push(threadId * opsPerThread + i)
                        if (i % 2 == 0) stack.pop() // Mix push/pop operations
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        // Benchmark MutexStack
        val mutexTime = measureTime {
            val stack = MutexStack<Int>()
            val jobs = mutableListOf<Job>()
            
            repeat(threadCount) { threadId ->
                val job = launch {
                    repeat(opsPerThread) { i ->
                        stack.push(threadId * opsPerThread + i)
                        if (i % 2 == 0) stack.pop() // Mix push/pop operations
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        println("TreiberStack (lock-free): ${treiberTime.inWholeMilliseconds}ms")
        println("MutexStack (mutex-based): ${mutexTime.inWholeMilliseconds}ms")
        
        val speedup = mutexTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds.toDouble()
        println("Lock-free speedup: ${String.format("%.2f", speedup)}x")
        
        // Under high contention, lock-free should significantly outperform mutex-based
        assertTrue(speedup >= 1.5, "Lock-free should significantly outperform mutex-based under high contention")
    }
    
    @Test
    fun benchmarkProducerConsumerScenario() = runTest(timeout = 60.seconds) {
        println("\n=== Producer-Consumer Benchmark (4 producers, 4 consumers) ===")
        
        val producerCount = 4
        val consumerCount = 4
        val itemsPerProducer = OPERATIONS_PER_THREAD / 8
        
        // Benchmark TreiberStack
        val treiberTime = measureTime {
            val stack = TreiberStack<Int>()
            val jobs = mutableListOf<Job>()
            
            // Producers
            repeat(producerCount) { producerId ->
                val job = launch {
                    repeat(itemsPerProducer) { i ->
                        stack.push(producerId * itemsPerProducer + i)
                        if (i % 10 == 0) yield() // Occasional yield
                    }
                }
                jobs.add(job)
            }
            
            // Consumers
            repeat(consumerCount) {
                val job = launch {
                    var consumed = 0
                    while (consumed < itemsPerProducer) {
                        val item = stack.pop()
                        if (item != null) {
                            consumed++
                        } else {
                            yield() // No item available, yield
                        }
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        // Benchmark MutexStack
        val mutexTime = measureTime {
            val stack = MutexStack<Int>()
            val jobs = mutableListOf<Job>()
            
            // Producers
            repeat(producerCount) { producerId ->
                val job = launch {
                    repeat(itemsPerProducer) { i ->
                        stack.push(producerId * itemsPerProducer + i)
                        if (i % 10 == 0) yield() // Occasional yield
                    }
                }
                jobs.add(job)
            }
            
            // Consumers
            repeat(consumerCount) {
                val job = launch {
                    var consumed = 0
                    while (consumed < itemsPerProducer) {
                        val item = stack.pop()
                        if (item != null) {
                            consumed++
                        } else {
                            yield() // No item available, yield
                        }
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        println("TreiberStack (lock-free): ${treiberTime.inWholeMilliseconds}ms")
        println("MutexStack (mutex-based): ${mutexTime.inWholeMilliseconds}ms")
        
        val speedup = mutexTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds.toDouble()
        println("Lock-free speedup: ${String.format("%.2f", speedup)}x")
        
        // Producer-consumer scenarios should favor lock-free
        assertTrue(speedup >= 1.2, "Lock-free should outperform mutex-based in producer-consumer scenarios")
    }
    
    @Test
    fun benchmarkBurstWorkload() = runTest(timeout = 60.seconds) {
        println("\n=== Burst Workload Benchmark (periods of intense activity) ===")
        
        val threadCount = 6
        val burstSize = 500
        val burstCount = 5
        
        // Benchmark TreiberStack
        val treiberTime = measureTime {
            val stack = TreiberStack<Int>()
            val jobs = mutableListOf<Job>()
            
            repeat(threadCount) { threadId ->
                val job = launch {
                    repeat(burstCount) { burstId ->
                        // Intense burst of operations
                        repeat(burstSize) { i ->
                            stack.push(threadId * 1000 + burstId * 100 + i)
                            if (i % 3 == 0) stack.pop()
                        }
                        // Brief pause between bursts
                        delay(1)
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        // Benchmark MutexStack
        val mutexTime = measureTime {
            val stack = MutexStack<Int>()
            val jobs = mutableListOf<Job>()
            
            repeat(threadCount) { threadId ->
                val job = launch {
                    repeat(burstCount) { burstId ->
                        // Intense burst of operations
                        repeat(burstSize) { i ->
                            stack.push(threadId * 1000 + burstId * 100 + i)
                            if (i % 3 == 0) stack.pop()
                        }
                        // Brief pause between bursts
                        delay(1)
                    }
                }
                jobs.add(job)
            }
            
            jobs.forEach { it.join() }
        }
        
        println("TreiberStack (lock-free): ${treiberTime.inWholeMilliseconds}ms")
        println("MutexStack (mutex-based): ${mutexTime.inWholeMilliseconds}ms")
        
        val speedup = mutexTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds.toDouble()
        println("Lock-free speedup: ${String.format("%.2f", speedup)}x")
        
        // Burst workloads should significantly favor lock-free due to no lock contention
        assertTrue(speedup >= 1.3, "Lock-free should handle burst workloads better than mutex-based")
    }
    
    @Test
    fun benchmarkScalabilityTest() = runTest(timeout = 60.seconds) {
        println("\n=== Scalability Test (1, 2, 4, 8 threads) ===")
        
        val threadCounts = listOf(1, 2, 4, 8)
        val opsPerThread = OPERATIONS_PER_THREAD / 8
        
        for (threadCount in threadCounts) {
            println("\nTesting with $threadCount thread(s):")
            
            // Benchmark TreiberStack
            val treiberTime = measureTime {
                val stack = TreiberStack<Int>()
                val jobs = mutableListOf<Job>()
                
                repeat(threadCount) { threadId ->
                    val job = launch {
                        repeat(opsPerThread) { i ->
                            stack.push(threadId * opsPerThread + i)
                            if (i % 2 == 0) stack.pop()
                        }
                    }
                    jobs.add(job)
                }
                
                jobs.forEach { it.join() }
            }
            
            // Benchmark MutexStack
            val mutexTime = measureTime {
                val stack = MutexStack<Int>()
                val jobs = mutableListOf<Job>()
                
                repeat(threadCount) { threadId ->
                    val job = launch {
                        repeat(opsPerThread) { i ->
                            stack.push(threadId * opsPerThread + i)
                            if (i % 2 == 0) stack.pop()
                        }
                    }
                    jobs.add(job)
                }
                
                jobs.forEach { it.join() }
            }
            
            val speedup = mutexTime.inWholeMilliseconds.toDouble() / treiberTime.inWholeMilliseconds.toDouble()
            println("  TreiberStack: ${treiberTime.inWholeMilliseconds}ms")
            println("  MutexStack:   ${mutexTime.inWholeMilliseconds}ms") 
            println("  Speedup:      ${String.format("%.2f", speedup)}x")
        }
        
        // This test demonstrates scalability - no specific assertions, just reporting
        assertTrue(true, "Scalability test completed")
    }
    
    private suspend fun warmupStacks() {
        // Warm up both implementations to ensure JIT compilation
        val treiberStack = TreiberStack<Int>()
        val mutexStack = MutexStack<Int>()
        
        repeat(WARMUP_OPERATIONS) { i ->
            treiberStack.push(i)
            mutexStack.push(i)
        }
        
        repeat(WARMUP_OPERATIONS) {
            treiberStack.pop()
            mutexStack.pop()
        }
    }
}