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

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.shabdiz.api.Platform;

/**
 * Provides platform-specific settings such as path separator and separator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class SimplePlatform implements Platform {

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
    public SimplePlatform(final String os_name, final char path_separator, final char separator, final String temp_dir) {

        this.os_name = os_name;
        this.path_separator = path_separator;
        this.separator = separator;
        this.temp_dir = addTailingSeparator(separator, temp_dir);
    }

    @Override
    public char getPathSeparator() {

        return path_separator;
    }

    @Override
    public char getSeparator() {

        return separator;
    }

    @Override
    public String getTempDirectory() {

        return temp_dir;
    }

    @Override
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

    protected static String addTailingSeparator(final char separator, final String path) {

        return path.endsWith(Character.toString(separator)) ? path : path + separator;
    }
}
