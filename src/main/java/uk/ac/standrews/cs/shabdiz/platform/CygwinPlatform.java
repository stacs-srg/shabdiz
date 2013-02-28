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
package uk.ac.standrews.cs.shabdiz.platform;

// TODO: Auto-generated Javadoc
/**
 * The Class CygwinPlatform.
 */
public class CygwinPlatform extends SimplePlatform {

    /** Default Cygwin operating system name. */
    public static final String CYGWIN_OS_NAME = "cygwin";

    /**
     * Instantiates a new cygwin platform.
     * 
     * @param os_name the os_name
     */
    public CygwinPlatform(final String os_name) {

        super(os_name, WINDOWS_PATH_SEPARATOR, UNIX_SEPARATOR, UnixPlatform.TEMP_DIR);
    }
}
