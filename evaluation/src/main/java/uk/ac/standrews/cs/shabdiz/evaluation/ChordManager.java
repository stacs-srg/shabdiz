package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;
import uk.ac.standrews.cs.utilities.archive.Diagnostic;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class ChordManager extends ExperimentManager {

    protected static final AttributeKey<IKey> NODE_KEY_KEY = new AttributeKey<IKey>();
    static final FileBasedCold FILE_BASED_COLD = new FileBasedCold();
    static final FileBasedWarm FILE_BASED_WARM = new FileBasedWarm();
    static final URLBased URL_BASED = new URLBased();
    static final MavenBasedCold MAVEN_BASED_COLD = new MavenBasedCold();
    static final MavenBasedWarm MAVEN_BASED_WARM = new MavenBasedWarm();
    private static final Logger LOGGER = LoggerFactory.getLogger(ChordManager.class);
    private static final Duration DEFAULT_BIND_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    private static final Duration DEFAULT_RETRY_DELAY = new Duration(10, TimeUnit.SECONDS);
    private static final long KEY_FACTORY_SEED = 0x585;
    private static final String STACHORD_MAVEN_ARTIFACT_COORDINATES = MavenDependencyResolver.toCoordinate("uk.ac.standrews.cs", "stachord", "2.0-SNAPSHOT");
    private static final DefaultArtifact STACHORD_MAVEN_ARTIFACT = new DefaultArtifact(STACHORD_MAVEN_ARTIFACT_COORDINATES);
    private final ChordNodeFactory node_factory = new ChordNodeFactory();
    private final SHA1KeyFactory key_factory = new SHA1KeyFactory(KEY_FACTORY_SEED);

    protected ChordManager() {

        super(NodeServer.class);
    }

    public ChordManager(Duration timeout) {

        super(timeout, NodeServer.class);
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();

        final InetSocketAddress previous_address = descriptor.getAttribute(ADDRESS_KEY);
        int port = previous_address == null ? 0 : previous_address.getPort();

        final IKey previous_key = descriptor.getAttribute(NODE_KEY_KEY);
        final IKey node_key = previous_key == null ? nextPeerKey() : previous_key;

        final Process node_process = process_builder.start(host, "-D" + Diagnostic.NONE, "-s" + host.getName() + ":" + port, "-x" + node_key.toString(Key.DEFAULT_RADIX));
        LOGGER.debug("waiting for properties of process on host {}...", host);

        final Properties properties = getPropertiesFromProcess(node_process);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(properties.getProperty(NodeServer.ADDRESS_PROPERTY_KEY));
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), address.getPort()));

        descriptor.setAttribute(NODE_KEY_KEY, node_key);
        descriptor.setAttribute(ADDRESS_KEY, address);
        descriptor.setAttribute(PROCESS_KEY, node_process);
        descriptor.setAttribute(PID_KEY, pid);

        return node_reference;
    }

    private IChordRemoteReference bindWithRetry(final InetSocketAddress address) throws InterruptedException, ExecutionException, TimeoutException {

        return bindWithRetry(address, DEFAULT_BIND_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    private IChordRemoteReference bindWithRetry(final InetSocketAddress address, final Duration timeout, final Duration retry_interval) throws InterruptedException, ExecutionException, TimeoutException {

        return TimeoutExecutorService.retry(new Callable<IChordRemoteReference>() {

            @Override
            public IChordRemoteReference call() throws Exception {

                return node_factory.bindToNode(address);
            }
        }, timeout, retry_interval);
    }

    private synchronized IKey nextPeerKey() {

        return key_factory.generateKey();
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final IChordRemoteReference reference = descriptor.getApplicationReference();
        reference.ping();
    }

    static class URLBased extends ChordManager {

        @Override
        public String toString() {

            return "ChordManager.URL";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);

            final List<URL> dependency_urls = resolver.resolveAsRemoteURLs(STACHORD_MAVEN_ARTIFACT);
            for (URL url : dependency_urls) {
                process_builder.addURL(url);
            }
        }
    }

    static class FileBasedCold extends ChordManager {

        FileBasedCold() {

        }

        public FileBasedCold(final Duration timeout) {

            super(timeout);
        }

        @Override
        public String toString() {

            return "ChordManager.FileCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<File> dependency_files = resolver.resolve(STACHORD_MAVEN_ARTIFACT_COORDINATES);
            for (File file : dependency_files) {
                process_builder.addFile(file);
            }
        }
    }

    static class FileBasedWarm extends ChordManager {

        FileBasedWarm() {

        }

        public FileBasedWarm(final Duration timeout) {

            super(timeout);
        }

        @Override
        public String toString() {

            return "ChordManager.FileWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            final List<File> dependency_files = resolver.resolve(STACHORD_MAVEN_ARTIFACT_COORDINATES);
            LOGGER.info("resolved chord dependencies locally. total of {} files", dependency_files.size());

            final String dependencies_home = "/tmp/chord_dependencies/";
            LOGGER.info("uploading chord dependencies to {} hosts at {}", network.size(), dependencies_home);
            uploadToAllHosts(network, dependency_files, dependencies_home, OVERRIDE_FILES_IN_WARM);

            for (File file : dependency_files) {
                process_builder.addRemoteFile(dependencies_home + file.getName());
            }
        }
    }

    static class MavenBasedCold extends ChordManager {

        MavenBasedCold() {

        }

        public MavenBasedCold(final Duration timeout) {

            super(timeout);
        }

        @Override
        public String toString() {

            return "ChordManager.MavenCold";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);
            LOGGER.info("Attempting to remove all shabdiz cached files on {} hosts", network.size());
            clearCachedShabdizFilesOnAllHosts(network);
            process_builder.addMavenDependency(STACHORD_MAVEN_ARTIFACT_COORDINATES);
        }
    }

    static class MavenBasedWarm extends ChordManager {

        MavenBasedWarm() {

        }

        public MavenBasedWarm(final Duration timeout) {

            super(timeout);
        }

        @Override
        public String toString() {

            return "ChordManager.MavenWarm";
        }

        @Override
        protected void configure(final ApplicationNetwork network) throws Exception {

            super.configure(network);

            LOGGER.info("Attempting to resolve {} on {} hosts", STACHORD_MAVEN_ARTIFACT_COORDINATES, network.size());
            resolveMavenArtifactOnAllHosts(network, STACHORD_MAVEN_ARTIFACT_COORDINATES);
            process_builder.addMavenDependency(STACHORD_MAVEN_ARTIFACT_COORDINATES);
        }
    }
}
