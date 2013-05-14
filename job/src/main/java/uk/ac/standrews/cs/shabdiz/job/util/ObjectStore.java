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

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Provides an interface to store and retrieve objects.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class ObjectStore {

    /** The storage. */
    public static final Map<Object, Object> STORE = new ConcurrentSkipListMap<Object, Object>();

    private ObjectStore() {

    }

    //    // -------------------------------------------------------------------------------------------------------------------------------
    //
    //    /**
    //        * Gets the mapped value to the given key.
    //        *
    //        * @param key the key
    //        * @return the value mapped to the given key. <code>null</code> if no such mapping exists
    //        */
    //    public static Object get(final Object key) {
    //
    //        return STORE.get(key);
    //    }
    //
    //    /**
    //     * Stores the given key/value pair.
    //     *
    //     * @param value the value to store
    //     */
    //    public static void put(final Object key, final Object value) {
    //
    //        STORE.put(key, value);
    //    }
    //
    //    /**
    //     * Removes the key/value entry.
    //     *
    //     * @param key the key
    //     * @return the value mapped to the given key, <code>null</code> if no such mapping exists
    //     */
    //    public Object remove(final Object key) {
    //
    //        return STORE.remove(key);
    //    }
}
