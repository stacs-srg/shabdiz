/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.job.util;

import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for storing key-value paris in the current JVM.
 * A use of this class is when two or more jobs depend on the result of each other.
 * Using this class a job can store its result without transmitting it back to the submitter, and later the result can be accessed by some other job that is submitted to the same worker.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Attributes {

    private static final ConcurrentHashMap<AttributeKey<?>, Object> STORE = new ConcurrentHashMap<AttributeKey<?>, Object>();

    private Attributes() {

    }

    /**
     * Gets the mapped value to the given key or {@code null} if no such mapping exists.
     *
     * @param key the key of the value to be retrieved
     * @return the value mapped to the given key, or {@code null} if no such mapping exists
     * @see ConcurrentHashMap#get(Object)
     */
    @SuppressWarnings("unchecked")
    public static <Value> Value get(final AttributeKey<Value> key) {
        return (Value) STORE.get(key);
    }

    /**
     * Stores the given key/value pair.
     * Both key and value must be {@code non-null}.
     *
     * @param key the key to which the value is associated
     * @param value the value to store
     * @see ConcurrentHashMap#put(Object, Object)
     */

    @SuppressWarnings("unchecked")
    public static <Value> Value put(final AttributeKey<Value> key, final Value value) {

        return (Value) STORE.put(key, value);
    }

    /**
     * Removes the key/value pair from this store to which the given key is mapped.
     *
     * @param key the key of the value to be removed
     * @return previous value associated to the given key, or {@code null} if no such mapping found
     * @see ConcurrentHashMap#remove(Object)
     */
    @SuppressWarnings("unchecked")
    public <Value> Value remove(final AttributeKey<Value> key) {

        return (Value) STORE.remove(key);
    }
}
