/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
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
package uk.ac.standrews.cs.shabdiz.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import uk.ac.standrews.cs.nds.madface.URL;

/**
 * Utility class that provides application library urls used by Shabdiz.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LibraryUtil {

    private static final String BUILD_SERVER_INDEX_URL = "https://builds.cs.st-andrews.ac.uk/";

    private static Set<URL> shabdiz_application_lib_urls;

    private LibraryUtil() {

    }

    /**
     * Gets the set of application library URLs required by Shabdiz.
     *
     * @return the set of application library URLs required by Shabdiz
     * @throws IOException if one of the URLs are not reachable.
     */
    public static synchronized Set<URL> getShabdizApplicationLibraryURLs() throws IOException {

        if (shabdiz_application_lib_urls == null) {
            initShabdizApplicationLibURLs();
        }

        return shabdiz_application_lib_urls;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private static synchronized void initShabdizApplicationLibURLs() throws IOException {

        shabdiz_application_lib_urls = new HashSet<URL>();

        shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/hudson_tools/lastSuccessfulBuild/artifact/lib/junit-4.8.2.jar"));
        shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/shabdiz/lastSuccessfulBuild/artifact/lib/json.jar"));
        shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/trombone/lastSuccessfulBuild/artifact/lib/mindterm.jar"));
    }
}
