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
package uk.ac.standrews.cs.shabdiz.evaluation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.json.JsonClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.echo.DefaultEcho;
import uk.ac.standrews.cs.shabdiz.example.echo.Echo;
import uk.ac.standrews.cs.shabdiz.example.echo.EchoBootstrap;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

abstract class EchoManager extends ExperimentManager {

    static final ClientFactory<Echo> ECHO_PROXY_FACTORY = new JsonClientFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoManager.class);
    private static final Duration DEFAULT_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final AttributeKey<InetSocketAddress> ADDRESS_KEY = new AttributeKey<InetSocketAddress>();
    private static final AttributeKey<Process> PROCESS_KEY = new AttributeKey<Process>();
    private static final AttributeKey<Integer> PID_KEY = new AttributeKey<Integer>();
    private static final String ECHO_MAVEN_ARTIFACT_COORDINATES = MavenDependencyResolver.toCoordinate(Constants.CS_GROUP_ID, Constants.SHABDIZ_EXAMPLES_ARTIFACT_ID, Constants.SHABDIZ_VERSION);
    private static final DefaultArtifact ECHO_MAVEN_ARTIFACT = new DefaultArtifact(ECHO_MAVEN_ARTIFACT_COORDINATES);

    static final FileBased FILE_BASED = new FileBased();
    static final URLBased URL_BASED = new URLBased();
    static final MavenBased MAVEN_BASED = new MavenBased();

    protected EchoManager() {

    }

    EchoManager(Duration timeout) {

        super(timeout);
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process echo_service_process = process_builder.start(host);
        final Properties properties = Bootstrap.readProperties(EchoBootstrap.class, echo_service_process, DEFAULT_DEPLOYMENT_TIMEOUT);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        final InetSocketAddress address = EchoBootstrap.getAddressProperty(properties);
        final Echo echo_proxy = ECHO_PROXY_FACTORY.get(address);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PROCESS_KEY, echo_service_process);
        descriptor.setAttribute(PID_KEY, pid);
        return echo_proxy;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            attemptTerminationByPID(descriptor);
            attemptTerminationByProcess(descriptor);
        }
        catch (final Exception e) {
            LOGGER.error("failed to kill echo application instance", e);
        }
    }

    private static void attemptTerminationByProcess(final ApplicationDescriptor descriptor) {

        final Process process = descriptor.getAttribute(PROCESS_KEY);
        if (process != null) {
            process.destroy();
        }
    }

    private static void attemptTerminationByPID(final ApplicationDescriptor descriptor) throws IOException, InterruptedException {

        final Integer pid = descriptor.getAttribute(PID_KEY);
        if (pid != null) {
            final Host host = descriptor.getHost();
            final String kill_command = Commands.KILL_BY_PROCESS_ID.get(host.getPlatform(), String.valueOf(pid));
            final Process kill = host.execute(kill_command);
            ProcessUtil.awaitNormalTerminationAndGetOutput(kill);
        }
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final String random_message = generateRandomString();
        final Echo echo_service = descriptor.getApplicationReference();
        final String echoed_message = echo_service.echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but received " + echoed_message); }
    }

    @Override
    protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

        process_builder.setMainClass(DefaultEcho.class);
    }

    private static String generateRandomString() {

        return UUID.randomUUID().toString();
    }

    static class URLBased extends EchoManager {

        URLBased() {

        }

        URLBased(final Duration timeout) throws Exception {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);
            final List<URL> dependenlcy_urls = resolver.resolveAsRemoteURLs(ECHO_MAVEN_ARTIFACT);
            configureURLBased(network, cold, dependenlcy_urls);
        }
    }

    static class FileBased extends EchoManager {

        FileBased() {

        }

        FileBased(final Duration timeout) throws Exception {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);
            final List<File> dependenlcy_files = resolver.resolve(ECHO_MAVEN_ARTIFACT_COORDINATES);
            configureFileBased(network, cold, dependenlcy_files);
        }
    }

    static class MavenBased extends EchoManager {

        MavenBased() {

        }

        MavenBased(final Duration timeout) throws Exception {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);
            configureMavenBased(network, cold, ECHO_MAVEN_ARTIFACT_COORDINATES);
        }
    }
}
