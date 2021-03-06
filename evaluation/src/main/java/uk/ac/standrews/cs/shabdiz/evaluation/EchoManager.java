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

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.sample_applications.echo.Echo;
import uk.ac.standrews.cs.sample_applications.echo.EchoBootstrap;
import uk.ac.standrews.cs.sample_applications.echo.EchoClient;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

abstract class EchoManager extends ExperimentManager {

    static final FileBasedCold FILE_BASED_COLD = new FileBasedCold();
    static final FileBasedWarm FILE_BASED_WARM = new FileBasedWarm();
    static final URLBased URL_BASED = new URLBased();
    static final MavenBasedWarm MAVEN_BASED_WARM = new MavenBasedWarm();
    static final MavenBasedCold MAVEN_BASED_COLD = new MavenBasedCold();
    static final String SAMPLE_APPLICATIONS_GROUP_ID = "uk.ac.standrews.cs.sample_applications";
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoManager.class);
    private static final String ECHO_MAVEN_ARTIFACT_COORDINATES = MavenDependencyResolver.toCoordinate(SAMPLE_APPLICATIONS_GROUP_ID, "echo", "1.0");
    private static final DefaultArtifact ECHO_MAVEN_ARTIFACT = new DefaultArtifact(ECHO_MAVEN_ARTIFACT_COORDINATES);

    protected EchoManager() {

        super(EchoBootstrap.class);
    }

    protected EchoManager(Duration timeout) {

        super(timeout, EchoBootstrap.class);
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final InetSocketAddress previous_address = descriptor.getAttribute(ADDRESS_KEY);
        final int port = previous_address == null ? 0 : previous_address.getPort();

        final Process echo_service_process = process_builder.start(host, String.valueOf(port));
        LOGGER.debug("waiting for properties of process on host {}...", host);

        final Properties properties = getPropertiesFromProcess(echo_service_process);
        final Integer pid = EchoBootstrap.getPIDProperty(properties);
        final String address_as_string = EchoBootstrap.getAddressPropertyAsString(properties);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final EchoClient echo_client = new EchoClient(address);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PROCESS_KEY, echo_service_process);
        descriptor.setAttribute(PID_KEY, pid);
        return echo_client;
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final String random_message = generateRandomString();
        final Echo echo_service = descriptor.getApplicationReference();
        final String echoed_message = echo_service.echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but received " + echoed_message); }
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
            final List<File> dependency_files = resolver.resolve(ECHO_MAVEN_ARTIFACT_COORDINATES);
            for (File file : dependency_files) {
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

            final List<File> dependency_files = resolver.resolve(ECHO_MAVEN_ARTIFACT_COORDINATES);
            LOGGER.info("resolved echo dependencies locally. total of {} files", dependency_files.size());

            final String dependencies_home = "/tmp/echo_dependencies/";
            LOGGER.info("uploading echo dependencies to {} hosts at {}", network.size(), dependencies_home);
            uploadToAllHosts(network, dependency_files, dependencies_home, OVERRIDE_FILES_IN_WARM);

            for (File file : dependency_files) {
                process_builder.addRemoteFile(dependencies_home + file.getName());
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
            LOGGER.info("Attempting to remove all shabdiz cached files on {} hosts", network.size());
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
            LOGGER.info("Attempting to resolve {} on {} hosts", ECHO_MAVEN_ARTIFACT_COORDINATES, network.size());
            resolveMavenArtifactOnAllHosts(network, ECHO_MAVEN_ARTIFACT_COORDINATES);
            process_builder.addMavenDependency(ECHO_MAVEN_ARTIFACT_COORDINATES);
        }
    }
}
