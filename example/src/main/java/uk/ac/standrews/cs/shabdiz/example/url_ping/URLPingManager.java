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
package uk.ac.standrews.cs.shabdiz.example.url_ping;

import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

/**
 * Performs pinging of a given URL by sending a {@code GET} request.
 * This manager does not support application instance termination and deployment.
 * The pinging is limited to HTTP URLs only.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class URLPingManager implements ApplicationManager {

    private static final String REQUEST_METHOD = "GET";
    private static final Logger LOGGER = LoggerFactory.getLogger(URLPingManager.class);
    private static final AttributeKey<URL> TARGET_KEY = new AttributeKey<URL>();

    /**
     * Sends a {@code GET} request to a target URL in order to discover its state.
     * The URL is considered to be in {@link ApplicationState#RUNNING} state if the connection results in a response code of {@link HttpURLConnection#HTTP_OK}.
     * 
     * @param descriptor the descriptor
     * @return {@inheritDoc}
     */
    @Override
    public ApplicationState probeState(final ApplicationDescriptor descriptor) {

        final URL target = descriptor.getAttribute(TARGET_KEY);
        if (target == null) {
            LOGGER.debug("unspecified target url in application descriptor: {}", descriptor);
            return ApplicationState.UNKNOWN;
        }

        return probeApplicationStateViaHttpConnection(target);
    }

    private ApplicationState probeApplicationStateViaHttpConnection(final URL target) {

        LOGGER.info("probing state of {}", target);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod(REQUEST_METHOD);
            connection.connect();
            final int response_code = connection.getResponseCode();
            switch (response_code) {
                case HttpURLConnection.HTTP_OK:
                    return ApplicationState.RUNNING;
                default:
                    return ApplicationState.UNKNOWN;
            }
        }
        catch (final Exception e) {
            LOGGER.info("failed to probe the state of {}", target);
            LOGGER.debug("failure while probing state", e);
            return ApplicationState.UNREACHABLE;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Sets the target URL for a given descriptor.
     * 
     * @param descriptor the descriptor to set the target URL of
     * @param url the target URL
     * @return the previous target URL of the given descriptor
     */
    public URL setTarget(final ApplicationDescriptor descriptor, final URL url) {

        return descriptor.setAttribute(TARGET_KEY, url);
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException("instance termination is not supported");
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException("instance deployment is not supported");
    }
}
