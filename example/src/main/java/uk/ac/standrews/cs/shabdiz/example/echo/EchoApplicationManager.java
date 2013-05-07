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

import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staticiser.jetson.ClientFactory;

class EchoApplicationManager extends AbstractApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoApplicationManager.class);
    private static final Duration DEFAULT_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    static final ClientFactory<Echo> ECHO_PROXY_FACTORY = new ClientFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    private static final AttributeKey<InetSocketAddress> ADDRESS_KEY = new AttributeKey<InetSocketAddress>();
    private static final AttributeKey<Integer> PID_KEY = new AttributeKey<Integer>();

    private final Random random;
    private final JavaProcessBuilder process_builder;

    EchoApplicationManager() {

        random = new Random();
        process_builder = new JavaProcessBuilder(DefaultEcho.class);
        process_builder.addCommandLineArgument(":0");
        process_builder.addCurrentJVMClasspath();
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process echo_service_process = process_builder.start(host);
        final String address_as_string = ProcessUtil.getValueFromProcessOutput(echo_service_process, DefaultEcho.ECHO_SERVICE_ADDRESS_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final String runtime_mxbean_name = ProcessUtil.getValueFromProcessOutput(echo_service_process, DefaultEcho.RUNTIME_MXBEAN_NAME_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final Integer pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mxbean_name);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final Echo echo_proxy = ECHO_PROXY_FACTORY.get(address);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PID_KEY, pid);

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
            final Integer pid = descriptor.getAttribute(PID_KEY);
            final Host host = descriptor.getHost();
            final String kill_command = Commands.KILL_BY_PROCESS_ID.get(host.getPlatform(), String.valueOf(pid));
            final Process kill = host.execute(kill_command);
            kill.waitFor();
            kill.destroy();
        }
        catch (final Exception e) {
            LOGGER.debug("failed to kill echo applciation instance", e);
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
