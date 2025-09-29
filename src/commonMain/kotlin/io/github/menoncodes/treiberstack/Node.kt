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

/**
 * Internal node structure for the Treiber Stack.
 * 
 * @param T the type of item stored in the node
 * @property item the data item stored in this node
 * @property next reference to the next node in the stack (null for bottom node)
 */
internal class Node<T>(
    val item: T,
    var next: Node<T>? = null
)