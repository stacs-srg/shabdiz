/*
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

import java.io.File;

/**
 * A singleton presentation of the local platform.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see #getInstance()
 */
public final class LocalPlatform extends SimplePlatform {

    private static final LocalPlatform LOCAL_PLATFORM_INSTANCE = new LocalPlatform();

    /**
     * Gets the single instance of the {@link LocalPlatform local platform}.
     * 
     * @return single instance of the {@link LocalPlatform local platform}
     */
    public static LocalPlatform getInstance() {

        return LOCAL_PLATFORM_INSTANCE;
    }

    private LocalPlatform() {

        super(System.getProperty("os.name"), File.pathSeparatorChar, File.separatorChar, System.getProperty("java.io.tmpdir"));
    }
}
