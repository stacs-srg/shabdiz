package uk.ac.standrews.cs.nds.madface;

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

    private static final String SLASH = "/";
    private final String url;
    private final java.net.URL real_url;

    public URL(final String url) throws IOException {

        this.url = url;
        real_url = new java.net.URL(url);

        checkLiveness();
    }

    public URL(final URL url_base, final String directory) throws IOException {

        this(url_base.url + directory);
    }

    private void checkLiveness() throws IOException {

        if (getProtocol().equalsIgnoreCase(HTTPS_PROTOCOL_NAME)) {

            /*
             * If an HTTPS url uses a self-signed certificate, utl connection results in SSLHandshakeException.
             * To avoid this exception, either
             *     - the server certificate needs to be added to the JVM's trusted keystore using keytool, or
             *     - the certificate verification mechanism needs to be disabled using a customised dummy trust manager.
             * 
             * The first solution seems like too much trouble since this method just need s to check the liveness of a URL, and it needs to be performed once on every machine that runs this code.
             * The second solution disables the certificate verification (temporarily) for the current JVM instance. Since we don't know who may use this class and for what purpose, this solution seems like a wrong thing to do.
             * 
             * So for the time being, i decided to just skip the liveness check for HTTPS urls.
             * 
             * For more information see http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
             */

            return;
        }

        final URLConnection connection = real_url.openConnection();
        connection.connect();
        final InputStream stream = connection.getInputStream();
        stream.close();
    }

    public static java.net.URL[] realUrlsAsArray(final Set<URL> urls) {

        final java.net.URL[] array = new java.net.URL[urls.size()];
        int count = 0;
        for (final URL url : urls) {
            array[count++] = url.real_url;
        }
        return array;
    }

    public static URL[] urlsAsArray(final Set<URL> urls) {

        return urls.toArray(new URL[0]);
    }

    public String getPath() {

        return url.substring(url.indexOf(SLASH) + 1);
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

        if (!(obj instanceof URL)) { return false; }
        final URL other = (URL) obj;

        return url.equals(other.url);
    }

    @Override
    public int hashCode() {

        return url.hashCode();
    }

    @Override
    public String toString() {

        return url;
    }
}
