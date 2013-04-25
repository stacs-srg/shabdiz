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
package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.io.IOException;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.jetson.JsonRpcServer;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleEchoService implements EchoService {

    private final InetSocketAddress local_address;
    private final JsonRpcServer server;
    static final String ECHO_SERVICE_ADDRESS_KEY = "ECHO_SERVICE_ADDRESS";

    public static void main(final String[] args) throws NumberFormatException, IOException {

        final String port_as_string = NetworkUtil.extractPortNumberAsString(args[0]);
        final InetSocketAddress local_address = NetworkUtil.getLocalIPv4InetSocketAddress(Integer.parseInt(port_as_string));
        final SimpleEchoService echo_service = new SimpleEchoService(local_address);
        ProcessUtil.printKeyValue(System.out, ECHO_SERVICE_ADDRESS_KEY, echo_service.getAddress());
    }

    public SimpleEchoService(final InetSocketAddress local_address) throws IOException {

        server = new JsonRpcServer(EchoService.class, this, new JsonFactory(new ObjectMapper()));
        server.setBindAddress(local_address);
        server.expose();
        this.local_address = server.getLocalSocketAddress();
    }

    public InetSocketAddress getAddress() {

        return local_address;
    }

    @Override
    public String echo(final String message) {

        return message;
    }

    @Override
    public void shutdown() {

        server.shutdown();
    }
}
