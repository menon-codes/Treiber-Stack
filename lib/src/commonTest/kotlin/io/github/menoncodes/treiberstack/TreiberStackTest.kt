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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Basic unit tests for TreiberStack implementation.
 */
class TreiberStackTest {
    
    @Test
    fun testEmptyStack() = runTest {
        val stack = TreiberStack<String>()
        
        assertTrue(stack.isEmpty())
        assertEquals(0, stack.size())
        assertNull(stack.peek())
        assertNull(stack.pop())
    }
    
    @Test
    fun testPushAndPop() = runTest {
        val stack = TreiberStack<String>()
        
        // Push elements
        stack.push("first")
        stack.push("second")
        stack.push("third")
        
        assertEquals(3, stack.size())
        assertEquals("third", stack.peek())
        
        // Pop elements (LIFO order)
        assertEquals("third", stack.pop())
        assertEquals("second", stack.pop())
        assertEquals("first", stack.pop())
        
        assertTrue(stack.isEmpty())
        assertNull(stack.pop())
    }
    
    @Test
    fun testPeekDoesNotModifyStack() = runTest {
        val stack = TreiberStack<Int>()
        
        stack.push(42)
        
        // Peek multiple times
        assertEquals(42, stack.peek())
        assertEquals(42, stack.peek())
        assertEquals(1, stack.size())
        
        // Verify pop still works
        assertEquals(42, stack.pop())
        assertTrue(stack.isEmpty())
    }
    
    @Test
    fun testMixedOperations() = runTest {
        val stack = TreiberStack<Int>()
        
        // Mixed push/pop operations
        stack.push(1)
        stack.push(2)
        assertEquals(2, stack.pop())
        
        stack.push(3)
        stack.push(4)
        assertEquals(4, stack.pop())
        assertEquals(3, stack.pop())
        assertEquals(1, stack.pop())
        
        assertTrue(stack.isEmpty())
    }
}