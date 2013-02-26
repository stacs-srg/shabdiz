/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

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
