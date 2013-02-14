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
package uk.ac.standrews.cs.nds.madface.config;

/**
 * Configuration constants.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class Config {

    private Config() {

    }

    /** The default path of wget on Linux. */
    public static final String DEFAULT_WGET_PATH_LINUX = "/usr/bin/wget";

    /** The default path of wget on Mac. */
    public static final String DEFAULT_WGET_PATH_MAC = "/usr/local/bin/wget";

    /** The default temp directory on Linux. */
    public static final String DEFAULT_TEMP_PATH_LINUX = "/tmp";

    /** The default temp directory on Mac. */
    public static final String DEFAULT_TEMP_PATH_MAC = "/tmp";

    /** The Java classpath separator character on Linux. */
    public static final String CLASS_PATH_SEPARATOR_LINUX = ":";

    /** The Java classpath separator character on Mac. */
    public static final String CLASS_PATH_SEPARATOR_MAC = ":";

    /** The Java classpath separator character on Windows. */
    public static final String CLASS_PATH_SEPARATOR_WINDOWS = ";";

    /** The file path separator character on Linux. */
    public static final String FILE_SEPARATOR_LINUX = "/";

    /** The file path separator character on Mac. */
    public static final String FILE_SEPARATOR_MAC = "/";

    /** The file path separator character on Windows. */
    public static final String FILE_SEPARATOR_WINDOWS = "\\";

    /** The Java compiler class. */
    public static final String JAVAC_MAIN_CLASS_NAME = "com.sun.tools.javac.Main";

    /** The Java compiler output class. */
    public static final String OUTPUT_CLASS_NAME = java.io.PrintWriter.class.getName();
}
