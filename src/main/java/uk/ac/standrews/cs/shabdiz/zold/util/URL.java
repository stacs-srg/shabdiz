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
package uk.ac.standrews.cs.shabdiz.zold.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Set;

/**
 * A simplified version of java.net.URL that does not perform name resolution on equals or hashcode. This improves performance when
 * storing instances in collections. See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class URL {

    /** The name of HTTPS protocol. */
    public static final String HTTPS_PROTOCOL_NAME = "https";

    private final String url;
    private final java.net.URL real_url;

    public URL(final String url) throws IOException {

        this.url = url;
        real_url = new java.net.URL(url);
        checkLiveness();
    }

    public static java.net.URL[] toArrayAsRealURLs(final Set<URL> urls) {

        final java.net.URL[] array = new java.net.URL[urls.size()];
        int count = 0;
        for (final URL url : urls) {
            array[count++] = url.real_url;
        }
        return array;
    }

    /**
     * Gets the path part of this URL.
     * 
     * @return the path part of this URL, or an empty string if one does not exist
     * @see java.net.URL#getPath()
     */
    public String getPath() {

        return real_url.getPath();
    }

    /**
     * Gets the protocol name.
     * 
     * @return the protocol name
     * @see java.net.URL#getProtocol()
     */
    public String getProtocol() {

        return real_url.getProtocol();
    }

    public java.net.URL getRealURL() {

        return real_url;
    }

    @Override
    public boolean equals(final Object obj) {

        return URL.class.isInstance(obj) && URL.class.cast(obj).url.equals(url);
    }

    @Override
    public int hashCode() {

        return url.hashCode();
    }

    @Override
    public String toString() {

        return url;
    }

    private void checkLiveness() throws IOException {

        if (!getProtocol().equalsIgnoreCase(HTTPS_PROTOCOL_NAME)) {

            /*
             * If an HTTPS url uses a self-signed certificate, check liveness results in SSLHandshakeException.
             * To avoid this exception, either
             *     - the server certificate needs to be added to the JVM's trusted keystore using keytool, or
             *     - the certificate verification mechanism needs to be disabled using a customised dummy trust manager.
             * 
             * The first solution seems like too much trouble since this method just need s to check the liveness of a URL, and it needs to be performed once on every machine that runs this code.
             * The second solution disables the certificate verification (temporarily) for the current JVM instance. Since we don't know who may use this class and for what purpose, this solution seems like the wrong thing to do.
             * 
             * So for the time being, i (masih) decided to just skip the liveness check for HTTPS URLs.
             * For more information see: http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
             */

            final URLConnection connection = real_url.openConnection();
            connection.connect();
            final InputStream stream = connection.getInputStream();
            stream.close();
        }
    }
}
