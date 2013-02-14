/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.nds.p2p.network;

import java.math.BigInteger;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;

import com.mindbright.jca.security.UnsupportedOperationException;

/**
 * Options for distribution of a set of keys in key space.
 * Further, provides utility methods for fixed-count key generation under different key distributions.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public enum KeyDistribution {

    /** Keys randomly distributed in the key space. */
    RANDOM,

    /** Keys evenly distributed around the key space. */
    EVEN,

    /** Keys clustered tightly in one region of the key space. */
    CLUSTERED,

    /** Keys logarithmically distributed around the key space. */
    LOGARITHMIC;

    // -------------------------------------------------------------------------------------------------------

    private static final Key DEFAULT_START_KEY = new Key(Key.MIN_KEY_VALUE);

    /**
     * Generates a fixed number of keys that are distributed according to this distribution starting from a key with value of {@link Key#MIN_KEY_VALUE}.
     * The start key is ignored if this key distribution is {@link #RANDOM}.
     * The generated keys under {@link #RANDOM} key distribution are non-deterministic.
     * The base of {@link Key#TWO} is used for {@link #LOGARITHMIC} key distribution.
     * 
     * @param number_of_keys the number of keys
     * @return the keys distributed according to this distribution
     */
    public IKey[] generateKeys(final int number_of_keys) {

        return generateKeys(DEFAULT_START_KEY, number_of_keys);
    }

    /**
     * Generates a fixed number of keys that are distributed according to this distribution starting from the given start key.
     * The start key is ignored if this key distribution is {@link #RANDOM}.
     * The generated keys under {@link #RANDOM} key distribution are non-deterministic.
     * The base of {@link Key#TWO} is used for {@link #LOGARITHMIC} key distribution.
     * 
     * @param start_key the start key
     * @param number_of_keys the number of keys
     * @return the keys distributed according to this distribution
     */
    public IKey[] generateKeys(final IKey start_key, final int number_of_keys) {

        final IKey[] keys;
        switch (this) {
            case RANDOM:
                keys = generateKeysRandomly(number_of_keys);
                break;
            case EVEN:
                keys = generateKeysEvenly(start_key, number_of_keys);
                break;
            case CLUSTERED:
                keys = generateKeysClustered(start_key, number_of_keys);
                break;
            case LOGARITHMIC:
                keys = generateKeysLogarithmically(start_key, number_of_keys, Key.TWO);
                break;
            default:
                throw new UnsupportedOperationException("key generation is not implementad for " + this);
        }

        return keys;
    }

    /**
     * Generates a fixed number of non-deterministic randomly distributed keys.
     * 
     * @param number_of_keys the number of keys
     * @return a fixed number of non-deterministic randomly distributed keys
     */
    public static IKey[] generateKeysRandomly(final int number_of_keys) {

        IKey[] generateRandomly = generateKeysRandomly(number_of_keys, new SHA1KeyFactory());
        return generateRandomly;
    }

    /**
     * Generates a fixed number of deterministic randomly distributed keys.
     * 
     * @param number_of_keys the number of keys
     * @param random_seed the random seed
     * @return a fixed number of non-deterministic randomly distributed keys
     */
    public static IKey[] generateKeysRandomly(final int number_of_keys, final long random_seed) {

        return generateKeysRandomly(number_of_keys, new SHA1KeyFactory(random_seed));
    }

    /**
     * Generates a fixed number of evenly distributed keys starting from a given start key.
     * The first element of generated keys is equal to the given start key.
     * 
     * @param start_key the start key
     * @param number_of_keys the number of keys
     * @return a fixed number of evenly distributed keys
     */
    public static IKey[] generateKeysEvenly(final IKey start_key, final int number_of_keys) {

        return generateKeysByIncrementation(number_of_keys, start_key, Key.KEYSPACE_SIZE.divide(new BigInteger(String.valueOf(number_of_keys))));
    }

    /**
     * Generates a fixed number of clustered keys starting from a given start key.
     * The first element of generated keys is equal to the given start key.
     * 
     * @param start_key the start key
     * @param number_of_keys the number of keys
     * @return a fixed number of clustered keys
     */
    public static IKey[] generateKeysClustered(final IKey start_key, final int number_of_keys) {

        return generateKeysByIncrementation(number_of_keys, start_key, BigInteger.ONE);
    }

    /**
     * Generates a fixed number of logarithmically distributed keys.
     * 
     * @param start_key the start key
     * @param number_of_keys the number of keys
     * @param base the logarithmic base
     * @return a fixed number of clustered keys
     */
    public static IKey[] generateKeysLogarithmically(final IKey start_key, final int number_of_keys, final BigInteger base) {

        final BigInteger start_value = start_key.keyValue();
        final IKey[] keys = new IKey[number_of_keys];
        BigInteger offset = Key.KEYSPACE_SIZE;
        for (int i = number_of_keys - 1; i >= 0; i--) {
            offset = offset.divide(base);
            keys[i] = new Key(start_value.add(offset));
        }
        return keys;
    }

    // -------------------------------------------------------------------------------------------------------

    private static IKey[] generateKeysByIncrementation(final int number_of_keys, final IKey start_key, final BigInteger increment_by) {

        final IKey[] keys = new IKey[number_of_keys];
        keys[0] = start_key;
        BigInteger next_key_value = start_key.keyValue();
        for (int i = 1; i < number_of_keys; i++) {
            next_key_value = next_key_value.add(increment_by);
            keys[i] = new Key(next_key_value);
        }
        return keys;
    }

    private static IKey[] generateKeysRandomly(final int number_of_keys, final SHA1KeyFactory random_key_factory) {

        final IKey[] keys = new IKey[number_of_keys];

        for (int i = 0; i < number_of_keys; i++) {
            keys[i] = random_key_factory.generateKey();
        }
        return keys;
    }
}
