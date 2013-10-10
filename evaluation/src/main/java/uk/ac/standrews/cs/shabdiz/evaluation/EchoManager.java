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
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.json.JsonClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.echo.Echo;
import uk.ac.standrews.cs.shabdiz.example.echo.EchoBootstrap;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.Duration;

abstract class EchoManager extends ExperimentManager {

    static final ClientFactory<Echo> ECHO_PROXY_FACTORY = new JsonClientFactory<Echo>(Echo.class, new JsonFactory(new ObjectMapper()));
    static final FileBasedCold FILE_BASED_COLD = new FileBasedCold();
    static final FileBasedWarm FILE_BASED_WARM = new FileBasedWarm();
    static final URLBased URL_BASED = new URLBased();
    static final MavenBasedWarm MAVEN_BASED_WARM = new MavenBasedWarm();
    static final MavenBasedCold MAVEN_BASED_COLD = new MavenBasedCold();
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoManager.class);
    private static final String ECHO_MAVEN_ARTIFACT_COORDINATES = MavenDependencyResolver.toCoordinate(Constants.CS_GROUP_ID, Constants.SHABDIZ_EXAMPLES_ARTIFACT_ID, Constants.SHABDIZ_VERSION);
    private static final DefaultArtifact ECHO_MAVEN_ARTIFACT = new DefaultArtifact(ECHO_MAVEN_ARTIFACT_COORDINATES);

    protected EchoManager() {

    }

    protected EchoManager(Duration timeout) {

        super(timeout);
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();

        final InetSocketAddress previous_address = descriptor.getAttribute(ADDRESS_KEY);
        int port = previous_address == null ? 0 : previous_address.getPort();
        final Process echo_service_process = process_builder.start(host, String.valueOf(port));
        LOGGER.debug("waiting for properties of process on host {}...", host);
        final Properties properties = Bootstrap.readProperties(EchoBootstrap.class, echo_service_process, PROCESS_START_TIMEOUT);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        final InetSocketAddress address = EchoBootstrap.getAddressProperty(properties);
        final Echo echo_proxy = ECHO_PROXY_FACTORY.get(address);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PROCESS_KEY, echo_service_process);
        descriptor.setAttribute(PID_KEY, pid);
        return echo_proxy;
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final String random_message = generateRandomString();
        final Echo echo_service = descriptor.getApplicationReference();
        final String echoed_message = echo_service.echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but received " + echoed_message); }
    }

    @Override
    protected void configure(final ApplicationNetwork network) throws Exception {

        super.configure(network);
        process_builder.setMainClass(EchoBootstrap.class);
    }

    private static String generateRandomString() {

        return UUID.randomUUID().toString();
    }

    static class URLBased extends EchoManager {

        @Override
        public String toString() {

            return "EchoManager.URL";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<URL> dependenlcy_urls = resolver.resolveAsRemoteURLs(ECHO_MAVEN_ARTIFACT);
            for (URL url : dependenlcy_urls) {
                process_builder.addURL(url);
            }
        }
    }

    static class FileBasedCold extends EchoManager {

        @Override
        public String toString() {

            return "EchoManager.FileCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<File> dependenlcy_files = resolver.resolve(ECHO_MAVEN_ARTIFACT_COORDINATES);
            for (File file : dependenlcy_files) {
                process_builder.addFile(file);
            }
        }
    }

    static class FileBasedWarm extends EchoManager {

        @Override
        public String toString() {

            return "EchoManager.FileWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);

            final List<File> dependenlcy_files = resolver.resolve(ECHO_MAVEN_ARTIFACT_COORDINATES);
            LOGGER.info("resolved echo dependencies locally. total of {} files", dependenlcy_files.size());

            final String dependencies_home = "/tmp/chord_dependencies";
            LOGGER.info("uploading echo dependencies to {} hosts at {}", network.size(), dependencies_home);
            uploadToAllHosts(network, dependenlcy_files, dependencies_home, OVERRIDE_FILES_IN_WARN);

            for (File file : dependenlcy_files) {
                process_builder.addRemoteFile(dependencies_home + '/' + file.getName());
            }
        }
    }

    static class MavenBasedCold extends EchoManager {

        @Override
        public String toString() {

            return "EchoManager.MavenCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            LOGGER.info("Attemting to remove all shabdiz cached files on {} hosts", network.size());
            clearCachedShabdizFilesOnAllHosts(network);
            process_builder.addMavenDependency(ECHO_MAVEN_ARTIFACT_COORDINATES);
        }
    }

    static class MavenBasedWarm extends EchoManager {

        @Override
        public String toString() {

            return "EchoManager.MavenWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            LOGGER.info("Attemting to resolve {} on {} hosts", ECHO_MAVEN_ARTIFACT_COORDINATES, network.size());
            resolveMavenArtifactOnAllHosts(network, ECHO_MAVEN_ARTIFACT_COORDINATES);
            process_builder.addMavenDependency(ECHO_MAVEN_ARTIFACT_COORDINATES);
        }
    }
}
