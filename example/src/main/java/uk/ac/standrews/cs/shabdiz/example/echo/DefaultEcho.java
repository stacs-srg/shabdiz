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

package uk.ac.standrews.cs.shabdiz.example.echo;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
import org.mashti.jetson.json.JsonServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a remotely accessible {@link Echo} service.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultEcho implements Echo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEcho.class);
    private static final ServerFactory<Echo> SERVER_FACTORY = new JsonServerFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    private final InetSocketAddress local_address;
    private final Server server;

    /**
     * Instantiates a new default echo and exposes an echo server on the given address.
     *
     * @param local_address the address on which this service is exposed
     * @throws IOException Signals that an I/O exception has occurred
     */
    public DefaultEcho(final InetSocketAddress local_address) throws IOException {

        server = SERVER_FACTORY.createServer(this);
        server.setBindAddress(local_address);
        server.expose();
        this.local_address = server.getLocalSocketAddress();
    }

    @Override
    public CompletableFuture<String> echo(final String message) {

        return CompletableFuture.completedFuture(message);
    }

    /** Shuts down this remote interface. */
    public void shutdown() {

        try {
            server.unexpose();
        }
        catch (final IOException e) {
            LOGGER.error("failed to unexpose echo server", e);
        }
    }

    InetSocketAddress getAddress() {

        return local_address;
    }
}
