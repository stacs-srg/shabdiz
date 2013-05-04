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

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staticiser.jetson.ClientFactory;
import com.staticiser.jetson.exception.JsonRpcException;

class EchoApplicationManager extends AbstractApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoApplicationManager.class);
    private static final ClientFactory<Echo> ECHO_PROXY_FACTORY = new ClientFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    private final Random random;
    private final JavaProcessBuilder process_builder;

    private static final Duration DEFAULT_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    EchoApplicationManager() {

        random = new Random();
        process_builder = new JavaProcessBuilder(DefaultEcho.class);
        process_builder.addCommandLineArgument(":0");
        process_builder.addCurrentJVMClasspath();
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final EchoApplicationDescriptor echo_descriptor = (EchoApplicationDescriptor) descriptor;
        final Host host = echo_descriptor.getHost();
        final Process echo_service_process = process_builder.start(host);
        final String address_as_string = ProcessUtil.getValueFromProcessOutput(echo_service_process, DefaultEcho.ECHO_SERVICE_ADDRESS_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final InetSocketAddress address = Marshaller.getAddress(address_as_string);
        final Echo echo_proxy = ECHO_PROXY_FACTORY.get(address);
        echo_descriptor.setAddress(address);
        return echo_proxy;
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final String random_message = generateRandomString();
        final Echo echo_service = descriptor.getApplicationReference();
        final String echoed_message = echo_service.echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but recieved " + echoed_message); }

    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            final Echo echo_service = descriptor.getApplicationReference();
            echo_service.shutdown();
        }
        catch (final JsonRpcException e) {
            LOGGER.trace("expected rpc error occured as a result of echo service shutdown", e);
        }
        finally {
            super.kill(descriptor);
        }
    }

    private String generateRandomString() {

        synchronized (random) {
            final StringBuilder builder = new StringBuilder();
            builder.append("RANDOM MESSAGE: ");
            builder.append(random.nextLong());
            return String.valueOf(builder.toString());
        }
    }
}
