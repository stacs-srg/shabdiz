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
