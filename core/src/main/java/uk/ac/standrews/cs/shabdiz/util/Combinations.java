/*
 * Copyright 2013 Masih Hajiarabderkani
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides utility methods to generate combinations.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Combinations {

    private Combinations() {

    }

    /**
     * Generates combinations of an array of arguments recursively.
     * The rows in the given arguments specify different arguments and the columns specify different cases of a configurable.
     * 
     * @param args the arguments
     * @return the list containing all the combinations of the given arguments
     */
    public static List<Object[]> generateArgumentCombinations(final Object[][] args) {

        return generateArgumentCombinations(args, Object.class);
    }

    /**
     * Generates combinations of an array of arguments recursively.
     * The rows in the given arguments specify different arguments and the columns specify different cases of a configurable.
     *
     * @param <T> the generic type
     * @param args the arguments
     * @param type the type
     * @return the list containing all the combinations of the given arguments
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T[]> generateArgumentCombinations(final T[][] args, final Class<T> type) {

        final List<T[]> combinations = new ArrayList<T[]>();
        // We need the type parameter because getClass().getComponentType() is no good when there is a 2D array of type Object with elements of type T[].
        populateCombinations(0, args, (T[]) Array.newInstance(type, args.length), combinations);
        return combinations;
    }

    private static <T> void populateCombinations(final int index, final T[][] args, final T[] combination_so_far, final Collection<T[]> combinations) {

        if (index < args.length) {
            for (int i = 0; i < args[index].length; i++) {
                combination_so_far[index] = args[index][i];
                populateCombinations(index + 1, args, combination_so_far, combinations);
            }
        }
        else {
            combinations.add(combination_so_far.clone());
        }
    }
}
