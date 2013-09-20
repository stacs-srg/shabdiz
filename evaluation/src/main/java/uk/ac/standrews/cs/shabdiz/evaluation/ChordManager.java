package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.aether.artifact.DefaultArtifact;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class ChordManager extends ExperimentManager {

    private static final Duration DEFAULT_BIND_TIMEOUT = new Duration(20, TimeUnit.SECONDS);
    private static final Duration DEFAULT_RETRY_DELAY = new Duration(3, TimeUnit.SECONDS);
    private static final Duration PROCESS_START_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final AttributeKey<Process> PEER_PROCESS_KEY = new AttributeKey<Process>();
    private static final AttributeKey<Integer> PEER_PROCESS_PID_KEY = new AttributeKey<Integer>();
    private static final long KEY_FACTORY_SEED = 0x585;
    private static final String STACHORD_MAVEN_ARTIFACT_COORDINATES = MavenDependencyResolver.toCoordinate(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT");
    private static final DefaultArtifact STACHORD_MAVEN_ARTIFACT = new DefaultArtifact(STACHORD_MAVEN_ARTIFACT_COORDINATES);
    private final ChordNodeFactory node_factory = new ChordNodeFactory();
    private final SHA1KeyFactory key_factory = new SHA1KeyFactory(KEY_FACTORY_SEED);

    static final FileBased FILE_BASED = new FileBased();
    static final URLBased URL_BASED = new URLBased();
    static final MavenBased MAVEN_BASED = new MavenBased();

    protected ChordManager() {

    }

    public ChordManager(Duration timeout) {

        super(timeout);
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process node_process = process_builder.start(host, "-s" + host.getName() + ":0", "-x" + nextPeerKey().toString(Key.DEFAULT_RADIX));
        final Properties properties = Bootstrap.readProperties(NodeServer.class, node_process, PROCESS_START_TIMEOUT);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(properties.getProperty(NodeServer.CHORD_NODE_LOCAL_ADDRESS_KEY));
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), address.getPort()));

        descriptor.setAttribute(PEER_PROCESS_KEY, node_process);
        descriptor.setAttribute(PEER_PROCESS_PID_KEY, pid);
        return node_reference;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            killByProcessID(descriptor);
        }
        finally {
            destroyProcess(descriptor);
        }
    }

    @Override
    protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

        process_builder.setMainClass(NodeServer.class);
    }

    private IChordRemoteReference bindWithRetry(final InetSocketAddress address) throws InterruptedException, ExecutionException, TimeoutException {

        return bindWithRetry(address, DEFAULT_BIND_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    private IChordRemoteReference bindWithRetry(final InetSocketAddress address, final Duration timeout, final Duration delay) throws InterruptedException, ExecutionException, TimeoutException {

        return TimeoutExecutorService.awaitCompletion(new Callable<IChordRemoteReference>() {

            @Override
            public IChordRemoteReference call() throws Exception {

                Exception error;
                do {
                    delay.sleep();
                    try {
                        return node_factory.bindToNode(address);
                    }
                    catch (final Exception e) {
                        error = e;
                    }
                }
                while (!Thread.currentThread().isInterrupted());
                throw error;
            }
        }, timeout);
    }

    private synchronized IKey nextPeerKey() {

        return key_factory.generateKey();
    }

    private void killByProcessID(final ApplicationDescriptor descriptor) throws IOException, InterruptedException {

        final Integer pid = descriptor.getAttribute(PEER_PROCESS_PID_KEY);
        if (pid != null) {
            final Host host = descriptor.getHost();
            ProcessUtil.killProcessOnHostByPID(host, pid);
        }
    }

    private void destroyProcess(final ApplicationDescriptor descriptor) {

        final Process process = descriptor.getAttribute(PEER_PROCESS_KEY);
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final IChordRemoteReference reference = descriptor.getApplicationReference();
        reference.ping();
    }

    static class MavenBased extends ChordManager {

        MavenBased() {

        }

        public MavenBased(final Duration timeout) {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);
            configureMavenBased(network, cold, STACHORD_MAVEN_ARTIFACT_COORDINATES);
        }

        @Override
        public String toString() {

            return "ChordManager.Maven";
        }
    }

    static class URLBased extends ChordManager {

        URLBased() {

        }

        public URLBased(final Duration timeout) {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);

            final List<URL> dependenlcy_urls = resolver.resolveAsRemoteURLs(STACHORD_MAVEN_ARTIFACT);
            configureURLBased(network, cold, dependenlcy_urls);
        }

        @Override
        public String toString() {

            return "ChordManager.URL";
        }
    }

    static class FileBased extends ChordManager {

        FileBased() {

        }

        public FileBased(final Duration timeout) {

            super(timeout);
        }

        @Override
        protected void configure(final ApplicationNetwork network, final boolean cold) throws Exception {

            super.configure(network, cold);
            final List<File> dependenlcy_files = resolver.resolve(STACHORD_MAVEN_ARTIFACT_COORDINATES);
            configureFileBased(network, cold, dependenlcy_files, "chord");
        }

        @Override
        public String toString() {

            return "ChordManager.File";
        }
    }
}
