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
package uk.ac.standrews.cs.shabdiz;

import java.io.File;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;

/**
 * Provides platform-specific settings such as path separator and separator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Platform {

    /** The local platform. */
    public static final Platform LOCAL = new LocalPlatform();

    /** Default Windows operating system name. */
    public static final String WINDOWS_OS_NAME = "windows";

    /** Default Cygwin operating system name. */
    public static final String CYGWIN_OS_NAME = "cygwin";

    private static final char WINDOWS_PATH_SEPARATOR = ';';
    private static final char WINDOWS_SEPARATOR = '\\';
    private static final char UNIX_PATH_SEPARATOR = ':';
    private static final char UNIX_SEPARATOR = '/';
    private static final String UNIX_TEMP_DIR = "/tmp/";

    /**
     * Constructs a {@link Platform} from the output that is produced by the execution of {@code uname} command.
     * Sets the platform operating system name to the given output.
     * If the given output contains {@link #WINDOWS_OS_NAME} it is assumed the platform is Windows.
     * If the given output contains {@link #CYGWIN_OS_NAME} it is assumed the platform is Cygwin.
     * Otherwise the platform is assumed to be Unix-based.
     * 
     * @param uname_output the output produced by the execution of {@code uname} commad
     * @return an isntance of {@link Platform} that represents Windowns, Cygwin or Unix platform
     */
    public static Platform fromUnameOutput(final String uname_output) {

        final String output = uname_output.toLowerCase().trim();
        if (output.contains(CYGWIN_OS_NAME)) { return new CygwinPlatform(output); }
        if (output.contains(WINDOWS_OS_NAME)) { return new WindowsPlatform(output); }
        return new UnixPlatform(output);
    }

    /**
     * Checks if a given platform presents a UNIX based platform.
     * 
     * @param target the target platform
     * @return true, if the path separator and separator of the target platform are equal to UNIX platform
     */
    public static boolean isUnixBased(final Platform target) {

        return target.getPathSeparator() == UNIX_PATH_SEPARATOR && target.getSeparator() == UNIX_SEPARATOR;
    }

    private final char path_separator;
    private final char separator;
    private final String temp_dir;
    private final String os_name;

    /**
     * Instantiates a new platform.
     * 
     * @param os_name the operating system name
     * @param path_separator the path separator
     * @param separator the separator
     * @param temp_dir the temp dir
     */
    public Platform(final String os_name, final char path_separator, final char separator, final String temp_dir) {

        this.os_name = os_name;
        this.path_separator = path_separator;
        this.separator = separator;
        this.temp_dir = temp_dir;
    }

    /**
     * Gets the path separator.
     * 
     * @return the path separator
     */
    public char getPathSeparator() {

        return path_separator;
    }

    /**
     * Gets the separator.
     * 
     * @return the separator
     */
    public char getSeparator() {

        return separator;
    }

    /**
     * Gets the temp directory.
     * 
     * @return the temp directory
     */
    public String getTempDirectory() {

        return temp_dir;
    }

    /**
     * Gets the operating system name.
     * 
     * @return the operating system name
     */
    public String getOperatingSystemName() {

        return os_name;
    }

    /**
     * Serialises this platform to JSON.
     * 
     * @return a JSON representation of this platform
     */
    public JSONObject toJSON() {

        final JSONObject json = new JSONObject();
        json.put("path_separator", path_separator);
        json.put("separator", separator);
        json.put("temp_dir", temp_dir);
        json.put("os_name", os_name);
        return json;
    }

    /**
     * Instantiates an instance of Platform from a JSON representation of a platform.
     * 
     * @param json the JSON representation of a platform
     * @return the deserialised platform
     * @throws JSONException if the given serialised JSON object is not deserialisable
     * @see #toJSON()
     */
    public static Platform fromJSON(final JSONObject json) throws JSONException {

        final char path_separator = (char) json.getInt("path_separator");
        final char separator = (char) json.getInt("separator");
        final String temp_dir = json.getString("temp_dir");
        final String os_name = json.getString("os_name");
        json.put("os_name", os_name);
        return new Platform(os_name, path_separator, separator, temp_dir);
    }

    private static class LocalPlatform extends Platform {

        protected LocalPlatform() {

            super(System.getProperty("os.name"), File.pathSeparatorChar, File.separatorChar, System.getProperty("java.io.tmpdir"));
        }
    }

    private static class UnixPlatform extends Platform {

        protected UnixPlatform(final String os_name) {

            super(os_name, UNIX_PATH_SEPARATOR, UNIX_SEPARATOR, UNIX_TEMP_DIR);
        }
    }

    private static class WindowsPlatform extends Platform {

        protected WindowsPlatform(final String os_name) {

            super(os_name, WINDOWS_PATH_SEPARATOR, WINDOWS_SEPARATOR, "");
        }
    }

    private static class CygwinPlatform extends Platform {

        protected CygwinPlatform(final String os_name) {

            super(os_name, WINDOWS_PATH_SEPARATOR, UNIX_SEPARATOR, UNIX_TEMP_DIR);
        }
    }
}
