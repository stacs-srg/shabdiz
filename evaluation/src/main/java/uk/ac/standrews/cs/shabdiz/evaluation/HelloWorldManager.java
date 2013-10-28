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
import java.net.URL;
import java.util.List;
import java.util.Properties;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.sample_applications.echo.Echo;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.util.Duration;

abstract class HelloWorldManager extends ExperimentManager {

    static final Artifact HELLO_WORLD_MAVEN_ARTIFACT = new DefaultArtifact("uk.ac.standrews.cs:hello_world:1.0");
    static final Artifact HELLO_WORLD_MAVEN_ARTIFACT_8M = new DefaultArtifact("uk.ac.standrews.cs:hello_world_8m:1.0");
    static final Artifact HELLO_WORLD_MAVEN_ARTIFACT_16M = new DefaultArtifact("uk.ac.standrews.cs:hello_world_16m:1.0");
    static final Artifact HELLO_WORLD_MAVEN_ARTIFACT_32M = new DefaultArtifact("uk.ac.standrews.cs:hello_world_32m:1.0");
    static final Artifact HELLO_WORLD_MAVEN_ARTIFACT_64M = new DefaultArtifact("uk.ac.standrews.cs:hello_world_64m:1.0");

    static final FileBasedCold FILE_BASED_COLD = new FileBasedCold(uk.ac.standrews.cs.sample_applications.hello_world.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT);
    static final FileBasedWarm FILE_BASED_WARM = new FileBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT);
    static final URLBased URL_BASED = new URLBased(uk.ac.standrews.cs.sample_applications.hello_world.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT);
    static final MavenBasedWarm MAVEN_BASED_WARM = new MavenBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT);
    static final MavenBasedCold MAVEN_BASED_COLD = new MavenBasedCold(uk.ac.standrews.cs.sample_applications.hello_world.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT);

    static final FileBasedCold FILE_BASED_COLD_8M = new FileBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_8m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_8M);
    static final FileBasedWarm FILE_BASED_WARM_8M = new FileBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_8m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_8M);
    static final URLBased URL_BASED_8M = new URLBased(uk.ac.standrews.cs.sample_applications.hello_world_8m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_8M);
    static final MavenBasedWarm MAVEN_BASED_WARM_8M = new MavenBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_8m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_8M);
    static final MavenBasedCold MAVEN_BASED_COLD_8M = new MavenBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_8m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_8M);

    static final FileBasedCold FILE_BASED_COLD_16M = new FileBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_16m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_16M);
    static final FileBasedWarm FILE_BASED_WARM_16M = new FileBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_16m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_16M);
    static final URLBased URL_BASED_16M = new URLBased(uk.ac.standrews.cs.sample_applications.hello_world_16m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_16M);
    static final MavenBasedWarm MAVEN_BASED_WARM_16M = new MavenBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_16m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_16M);
    static final MavenBasedCold MAVEN_BASED_COLD_16M = new MavenBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_16m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_16M);

    static final FileBasedCold FILE_BASED_COLD_32M = new FileBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_32m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_32M);
    static final FileBasedWarm FILE_BASED_WARM_32M = new FileBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_32m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_32M);
    static final URLBased URL_BASED_32M = new URLBased(uk.ac.standrews.cs.sample_applications.hello_world_32m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_32M);
    static final MavenBasedWarm MAVEN_BASED_WARM_32M = new MavenBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_32m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_32M);
    static final MavenBasedCold MAVEN_BASED_COLD_32M = new MavenBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_32m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_32M);

    static final FileBasedCold FILE_BASED_COLD_64M = new FileBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_64m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_64M);
    static final FileBasedWarm FILE_BASED_WARM_64M = new FileBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_64m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_64M);
    static final URLBased URL_BASED_64M = new URLBased(uk.ac.standrews.cs.sample_applications.hello_world_64m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_64M);
    static final MavenBasedWarm MAVEN_BASED_WARM_64M = new MavenBasedWarm(uk.ac.standrews.cs.sample_applications.hello_world_64m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_64M);
    static final MavenBasedCold MAVEN_BASED_COLD_64M = new MavenBasedCold(uk.ac.standrews.cs.sample_applications.hello_world_64m.PeriodicHelloWorld.class, HELLO_WORLD_MAVEN_ARTIFACT_64M);

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldManager.class);

    protected HelloWorldManager(Class<?> main_class) {

        super(main_class);
    }

    protected HelloWorldManager(Duration timeout, Class<?> main_class) {

        super(timeout, main_class);
    }

    @Override
    public Echo deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();

        final Process hello_world_process = process_builder.start(host);
        LOGGER.debug("waiting for properties of process on host {}...", host);

        final Properties properties = getPropertiesFromProcess(hello_world_process);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        descriptor.setAttribute(PROCESS_KEY, hello_world_process);
        descriptor.setAttribute(PID_KEY, pid);
        return null;
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final Process process = descriptor.getAttribute(PROCESS_KEY);
        try {
            final int exit_value = process.exitValue();
            throw new Exception("expected hello world process to be running, instead received exit value of " + exit_value);
        }
        catch (final IllegalThreadStateException e) {
            LOGGER.trace("process seems to be running on {}", descriptor);
        }
    }

    static class URLBased extends HelloWorldManager {

        private final Artifact maven_artifact;

        URLBased(Class<?> main_class, Artifact maven_artifact) {

            super(main_class);
            this.maven_artifact = maven_artifact;
        }

        @Override
        public String toString() {

            return maven_artifact.getArtifactId() + ".URL";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<URL> dependency_urls = resolver.resolveAsRemoteURLs(maven_artifact);
            for (URL url : dependency_urls) {
                process_builder.addURL(url);
            }
        }
    }

    static class FileBasedCold extends HelloWorldManager {

        private final Artifact maven_artifact;

        FileBasedCold(Class<?> main_class, Artifact maven_artifact) {

            super(main_class);
            this.maven_artifact = maven_artifact;
        }

        @Override
        public String toString() {

            return maven_artifact.getArtifactId() + ".FileCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<File> dependency_files = resolver.resolve(maven_artifact);
            for (File file : dependency_files) {
                process_builder.addFile(file);
            }
        }
    }

    static class FileBasedWarm extends HelloWorldManager {

        private final Artifact maven_artifact;
        private final String artifact_id;

        FileBasedWarm(Class<?> main_class, Artifact maven_artifact) {

            super(main_class);
            this.maven_artifact = maven_artifact;
            artifact_id = maven_artifact.getArtifactId();
        }

        @Override
        public String toString() {

            return artifact_id + ".FileWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);

            final List<File> dependency_files = resolver.resolve(maven_artifact);
            LOGGER.info("resolved {} dependencies locally. total of {} files", artifact_id, dependency_files.size());

            final String dependencies_home = "/tmp/" + artifact_id + "_dependencies/";
            LOGGER.info("uploading {} dependencies to {} hosts at {}", artifact_id, network.size(), dependencies_home);
            uploadToAllHosts(network, dependency_files, dependencies_home, OVERRIDE_FILES_IN_WARM);

            for (File file : dependency_files) {
                process_builder.addRemoteFile(dependencies_home + file.getName());
            }
        }
    }

    static class MavenBasedCold extends HelloWorldManager {

        private final Artifact maven_artifact;

        MavenBasedCold(Class<?> main_class, Artifact maven_artifact) {

            super(main_class);
            this.maven_artifact = maven_artifact;
        }

        @Override
        public String toString() {

            return maven_artifact.getArtifactId() + ".MavenCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            clearCachedShabdizFilesOnAllHosts(network);
            process_builder.addMavenDependency(maven_artifact.toString());
        }
    }

    static class MavenBasedWarm extends HelloWorldManager {

        private final Artifact maven_artifact;

        MavenBasedWarm(Class<?> main_class, Artifact maven_artifact) {

            super(main_class);
            this.maven_artifact = maven_artifact;
        }

        @Override
        public String toString() {

            return maven_artifact.getArtifactId() + ".MavenWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final String artifact_coordinate = maven_artifact.toString();
            resolveMavenArtifactOnAllHosts(network, artifact_coordinate);
            process_builder.addMavenDependency(artifact_coordinate);
        }
    }
}
