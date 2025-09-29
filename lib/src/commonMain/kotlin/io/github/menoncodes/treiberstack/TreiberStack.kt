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

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.yield

/**
 * A lock-free concurrent stack implementation using Treiber's algorithm.
 * 
 * This implementation uses Compare-And-Swap (CAS) operations to achieve thread safety
 * without traditional locking mechanisms, providing excellent performance in highly
 * concurrent scenarios.
 * 
 * The stack is implemented as a singly-linked list where the head of the list
 * represents the top of the stack. All operations (push and pop) are performed
 * at the head using atomic CAS operations.
 * 
 * @param T the type of elements stored in the stack
 */
class TreiberStack<T> {
    
    /**
     * Atomic reference to the top node of the stack.
     * null indicates an empty stack.
     */
    private val head: AtomicRef<Node<T>?> = atomic(null)
    
    /**
     * Pushes an item onto the top of the stack.
     * 
     * This operation is lock-free and thread-safe. It uses a retry loop with
     * Compare-And-Swap (CAS) operations to ensure atomicity even under high
     * contention scenarios.
     * 
     * @param item the item to push onto the stack
     */
    suspend fun push(item: T) {
        val newNode = Node(item)
        
        while (true) {
            val currentHead = head.value
            newNode.next = currentHead
            
            // Attempt to atomically update the head reference
            if (head.compareAndSet(currentHead, newNode)) {
                break // Success - exit the retry loop
            }
            
            // CAS failed due to contention, yield control and retry
            yield()
        }
    }
    
    /**
     * Pops and returns the top item from the stack.
     * 
     * This operation is lock-free and thread-safe. It uses a retry loop with
     * Compare-And-Swap (CAS) operations to ensure atomicity even under high
     * contention scenarios.
     * 
     * @return the top item from the stack, or null if the stack is empty
     */
    suspend fun pop(): T? {
        while (true) {
            val currentHead = head.value
            
            // Stack is empty
            if (currentHead == null) {
                return null
            }
            
            val nextNode = currentHead.next
            
            // Attempt to atomically update the head reference
            if (head.compareAndSet(currentHead, nextNode)) {
                return currentHead.item // Success - return the popped item
            }
            
            // CAS failed due to contention, yield control and retry
            yield()
        }
    }
    
    /**
     * Returns the top item from the stack without removing it.
     * 
     * @return the top item from the stack, or null if the stack is empty
     */
    fun peek(): T? {
        return head.value?.item
    }
    
    /**
     * Checks if the stack is empty.
     * 
     * @return true if the stack is empty, false otherwise
     */
    fun isEmpty(): Boolean {
        return head.value == null
    }
    
    /**
     * Returns the current size of the stack.
     * 
     * Note: This operation is O(n) as it traverses the entire stack.
     * In highly concurrent scenarios, the returned size might be approximate
     * due to concurrent modifications.
     * 
     * @return the number of elements in the stack
     */
    fun size(): Int {
        var count = 0
        var current = head.value
        
        while (current != null) {
            count++
            current = current.next
        }
        
        return count
    }
}