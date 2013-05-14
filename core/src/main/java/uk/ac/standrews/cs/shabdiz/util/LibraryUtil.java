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
package uk.ac.standrews.cs.shabdiz.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that provides application library urls used by Shabdiz.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LibraryUtil {

    private static final String BUILD_SERVER_INDEX_URL = "https://builds.cs.st-andrews.ac.uk/";
    private static IOException url_instantiation_exception;
    private static Set<URL> shabdiz_application_lib_urls = new HashSet<URL>();
    static {
        try {
            shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/shabdiz/lastSuccessfulBuild/artifact/bin/shabdiz.jar"));
            shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/nds/lastSuccessfulBuild/artifact/bin/nds.jar"));
            shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/hudson_tools/lastSuccessfulBuild/artifact/lib/junit-4.8.2.jar"));
            shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/shabdiz/lastSuccessfulBuild/artifact/lib/json.jar"));
            shabdiz_application_lib_urls.add(new URL(BUILD_SERVER_INDEX_URL + "job/trombone/lastSuccessfulBuild/artifact/lib/mindterm.jar"));
            url_instantiation_exception = null;
        }
        catch (final IOException e) {
            url_instantiation_exception = e;
        }
    }

    private LibraryUtil() {

    }

    /**
     * Gets the set of application library URLs required by Shabdiz.
     * 
     * @return the set of application library URLs required by Shabdiz
     * @throws IOException if one of the URLs are not reachable.
     */
    public static Set<URL> getShabdizApplicationLibraryURLs() throws IOException {

        if (url_instantiation_exception != null) { throw url_instantiation_exception; }
        return shabdiz_application_lib_urls;
    }
}
