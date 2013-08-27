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
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.json.JsonServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/**
 * Implements a remotely accessible {@link Echo} service.
 * This class contains a {@code main} method and can be used to start up a stand-alone Echo service.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultEcho implements Echo {

    static final String ECHO_SERVICE_ADDRESS_KEY = "ECHO_SERVICE_ADDRESS";
    static final String RUNTIME_MX_BEAN_NAME_KEY = "RUNTIME_MX_BEAN_NAME";
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

    /**
     * Starts a new instance of {@link DefaultEcho}.
     *
     * @param args expects the first entry to be the port number on which to listen for incoming connections
     * @throws NumberFormatException if the first entry in the arguments cannot be parsed to an integer
     * @throws IOException Signals that an I/O exception has occurred
     */
    public static void main(final String[] args) throws IOException {

        final String port_as_string = NetworkUtil.extractPortNumberAsString(args[0]);
        final InetSocketAddress local_address = NetworkUtil.getLocalIPv4InetSocketAddress(Integer.parseInt(port_as_string));
        final DefaultEcho echo_service = new DefaultEcho(local_address);
        ProcessUtil.printKeyValue(System.out, ECHO_SERVICE_ADDRESS_KEY, echo_service.getAddress());
        ProcessUtil.printKeyValue(System.out, RUNTIME_MX_BEAN_NAME_KEY, ManagementFactory.getRuntimeMXBean().getName());
    }

    @Override
    public String echo(final String message) {

        return message;
    }

    /**
     * Shuts down this remote interface.
     *
     * @throws RPCException signals that an RPC exception has occured
     */
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
