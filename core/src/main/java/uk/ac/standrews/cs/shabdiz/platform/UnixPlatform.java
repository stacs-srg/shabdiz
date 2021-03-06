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

package uk.ac.standrews.cs.shabdiz.platform;

/**
 * Presents a UNIX-based platform.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class UnixPlatform extends SimplePlatform {

    static final String DEFAULT_TEMP_DIR = "/tmp/";

    /**
     * Instantiates a new platform.
     *
     * @param os_name the os_name
     */
    public UnixPlatform(final String os_name) {

        this(os_name, DEFAULT_TEMP_DIR);
    }

    public UnixPlatform(final String os_name, String temp_dir) {

        super(os_name, UNIX_PATH_SEPARATOR, UNIX_SEPARATOR, temp_dir);
    }
}
