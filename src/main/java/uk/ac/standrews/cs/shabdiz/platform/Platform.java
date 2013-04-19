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

import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Stores the platform-specific settings of a {@link Host host}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see Host#getPlatform()
 * @see Platforms
 */
public interface Platform {

    /** The windows path separator. */
    char WINDOWS_PATH_SEPARATOR = ';';

    /** The windows separator. */
    char WINDOWS_SEPARATOR = '\\';

    /** The UNIX path separator. */
    char UNIX_PATH_SEPARATOR = ':';

    /** The UNIX separator. */
    char UNIX_SEPARATOR = '/';

    /**
     * Gets the path separator.
     * 
     * @return the path separator
     */
    char getPathSeparator();

    /**
     * Gets the separator.
     * 
     * @return the separator
     */
    char getSeparator();

    /**
     * Gets the path to this platform's {@code temp} directory.
     * 
     * @return the temporary directory
     */
    String getTempDirectory();

    /**
     * Gets the operating system name.
     * 
     * @return the operating system name
     */
    String getOperatingSystemName();
}
