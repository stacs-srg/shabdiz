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
package uk.ac.standrews.cs.shabdiz.new_api;

/**
 * The Interface Platform.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
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
     * Gets the temporary directory.
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
