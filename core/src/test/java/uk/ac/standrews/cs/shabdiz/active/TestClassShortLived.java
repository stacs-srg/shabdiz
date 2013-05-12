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
package uk.ac.standrews.cs.shabdiz.active;

/**
 * Test class for remote invocation.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class TestClassShortLived {

    /**
     * Prevent instantiation of utility class.
     */
    private TestClassShortLived() {

    }

    /**
     * Returns quickly.
     * 
     * @param args ignored
     * @throws Exception ignored
     */
    public static void main(final String[] args) throws Exception {

        System.out.println("test completed");
    }
}
