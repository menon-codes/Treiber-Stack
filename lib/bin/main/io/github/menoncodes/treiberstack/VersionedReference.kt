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
 * A versioned reference that helps prevent ABA problems.
 *
 * The ABA problem occurs when a value changes from A to B and back to A between the time a thread
 * reads it and when it attempts a CAS operation. By including a version number, we can detect when
 * the reference has been modified even if it contains the same value.
 *
 * @param T the type of reference stored
 * @property reference the actual reference to the object
 * @property version a monotonically increasing version number
 */
internal data class VersionedReference<T>(val reference: T?, val version: Long) {
    companion object {
        /** Creates a new versioned reference with version 0. */
        fun <T> initial(reference: T? = null): VersionedReference<T> {
            return VersionedReference(reference, 0L)
        }
    }

    /** Creates a new versioned reference with a new reference and incremented version. */
    fun withNewReference(newReference: T?): VersionedReference<T> {
        return VersionedReference(newReference, version + 1)
    }
}
