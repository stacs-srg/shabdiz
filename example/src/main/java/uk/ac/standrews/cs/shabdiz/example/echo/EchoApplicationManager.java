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
import com.staticiser.jetson.ClientFactory;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenManagedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

class EchoApplicationManager extends AbstractApplicationManager {

    static final ClientFactory<Echo> ECHO_PROXY_FACTORY = new ClientFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoApplicationManager.class);
    private static final Duration DEFAULT_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final AttributeKey<InetSocketAddress> ADDRESS_KEY = new AttributeKey<InetSocketAddress>();
    private static final AttributeKey<Process> PROCESS_KEY = new AttributeKey<Process>();
    private static final AttributeKey<Integer> PID_KEY = new AttributeKey<Integer>();
    public static final String ARGUMENTS = ":0";
    private final Random random;
    private final MavenManagedJavaProcessBuilder process_builder;

    EchoApplicationManager() {

        random = new Random();
        process_builder = new MavenManagedJavaProcessBuilder();
        process_builder.setMainClass(DefaultEcho.class);
        process_builder.addMavenDependency(Constants.SHABDIZ_GROUP_ID, Constants.SHABDIZ_EXAMPLES_ARTIFACT_ID, Constants.SHABDIZ_VERSION);
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process echo_service_process = process_builder.start(host, ARGUMENTS);
        final String address_as_string = ProcessUtil.scanProcessOutput(echo_service_process, DefaultEcho.ECHO_SERVICE_ADDRESS_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final String runtime_mx_bean_name = ProcessUtil.scanProcessOutput(echo_service_process, DefaultEcho.RUNTIME_MX_BEAN_NAME_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final Integer pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mx_bean_name);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final Echo echo_proxy = ECHO_PROXY_FACTORY.get(address);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PROCESS_KEY, echo_service_process);
        descriptor.setAttribute(PID_KEY, pid);

        return echo_proxy;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {

            final Integer pid = descriptor.getAttribute(PID_KEY);
            if (pid != null) {
                final Host host = descriptor.getHost();
                final String kill_command = Commands.KILL_BY_PROCESS_ID.get(host.getPlatform(), String.valueOf(pid));
                final Process kill = host.execute(kill_command);
                ProcessUtil.awaitNormalTerminationAndGetOutput(kill);
            }

            final Process process = descriptor.getAttribute(PROCESS_KEY);
            if (process != null) {
                process.destroy();
            }
        }
        catch (final Exception e) {
            LOGGER.debug("failed to kill echo application instance", e);
        }
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final String random_message = generateRandomString();
        final Echo echo_service = descriptor.getApplicationReference();
        final String echoed_message = echo_service.echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but received " + echoed_message); }

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
