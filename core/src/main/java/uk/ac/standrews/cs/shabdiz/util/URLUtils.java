package uk.ac.standrews.cs.shabdiz.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public final class URLUtils {

    private static final String HEAD_REQUEST_METHOD = "HEAD";

    /**
     * Pings a given {@code url} for availability. Effectively sends a HEAD request and returns {@code true} if the response code is in
     * the {@code 200} - {@code 399} range.
     * @param url the url to be pinged.
     * @param timeout_millis the timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return {@code true} if the given {@code url} has returned response code within the range of {@code 200} - {@code 399} on a HEAD request, {@code false} otherwise
     */
    public static boolean ping(String url, int timeout_millis) throws MalformedURLException {

        return ping(new URL(url), timeout_millis);
    }

    /**
     * Pings a given {@code url} for availability. Effectively sends a HEAD request and returns {@code true} if the response code is in
     * the {@code 200} - {@code 399} range.
     * @param url the url to be pinged.
     * @param timeout_millis the timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return {@code true} if the given {@code url} has returned response code within the range of {@code 200} - {@code 399} on a HEAD request, {@code false} otherwise
     */
    public static boolean ping(final URL url, final int timeout_millis) {

        try {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout_millis);
            connection.setReadTimeout(timeout_millis);
            connection.setRequestMethod(HEAD_REQUEST_METHOD);

            final int response_code = connection.getResponseCode();
            return HttpURLConnection.HTTP_OK <= response_code && response_code < HttpURLConnection.HTTP_BAD_REQUEST;
        }
        catch (IOException exception) {
            return false;
        }
    }
}
