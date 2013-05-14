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

package uk.ac.standrews.cs.shabdiz.util;

/**
 * A utility class for generating hash code from the hash code of class fields.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class HashCodeUtil {

    private static final int PRIME = 31;

    private HashCodeUtil() { }

    /**
     * Generates hash code from a given collection of hash codes.
     * The collection of hash codes must not be {@code null} and must have at least one element.
     *
     * @param member_hash_codes the hash codes to generate hash code from
     * @return a new hash code generated from the given collection of hash codes
     */
    public static int generate(final int... member_hash_codes) {

        if (member_hash_codes == null) { throw new NullPointerException("members hash codes must not be null"); }
        if (member_hash_codes.length < 1) { throw new IllegalArgumentException("at lease one hash code must be specified"); }

        int result = member_hash_codes[0];
        for (int i = 1; i < member_hash_codes.length; i++) {
            result = PRIME * result + member_hash_codes[i];
        }
        return result;
    }
}
