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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

/**
 * A traditional mutex-based stack implementation for benchmarking comparison.
 * 
 * This implementation uses a Kotlin coroutine Mutex to provide thread safety,
 * representing the traditional locking approach to concurrent data structures.
 * 
 * @param T the type of elements stored in the stack
 */
class MutexStack<T> {
    
    private val mutex = Mutex()
    private var head: Node<T>? = null
    private var stackSize = 0
    
    /**
     * Simple node structure for the linked list.
     */
    private data class Node<T>(val item: T, var next: Node<T>?)
    
    /**
     * Pushes an item onto the top of the stack.
     * Uses mutex locking for thread safety.
     * 
     * @param item the item to push onto the stack
     */
    suspend fun push(item: T) {
        mutex.withLock {
            val newNode = Node(item, head)
            head = newNode
            stackSize++
        }
        // Yield to simulate some work and allow other coroutines to run
        yield()
    }
    
    /**
     * Pops and returns the top item from the stack.
     * Uses mutex locking for thread safety.
     * 
     * @return the top item from the stack, or null if the stack is empty
     */
    suspend fun pop(): T? {
        return mutex.withLock {
            val currentHead = head
            if (currentHead != null) {
                head = currentHead.next
                stackSize--
                currentHead.item
            } else {
                null
            }
        }.also {
            // Yield to simulate some work and allow other coroutines to run
            yield()
        }
    }
    
    /**
     * Returns the top item from the stack without removing it.
     * 
     * @return the top item from the stack, or null if the stack is empty
     */
    suspend fun peek(): T? {
        return mutex.withLock {
            head?.item
        }
    }
    
    /**
     * Checks if the stack is empty.
     * 
     * @return true if the stack is empty, false otherwise
     */
    suspend fun isEmpty(): Boolean {
        return mutex.withLock {
            head == null
        }
    }
    
    /**
     * Returns the current size of the stack.
     * 
     * @return the number of elements in the stack
     */
    suspend fun size(): Int {
        return mutex.withLock {
            stackSize
        }
    }
}