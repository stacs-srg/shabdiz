package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ChordManager extends AbstractApplicationManager {

    private static final Duration DEFAULT_BIND_TIMEOUT = new Duration(20, TimeUnit.SECONDS);
    private static final Duration DEFAULT_RETRY_DELAY = new Duration(1, TimeUnit.SECONDS);
    private static final Duration PROCESS_START_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final AttributeKey<Process> PEER_PROCESS_KEY = new AttributeKey<Process>();
    private static final AttributeKey<Integer> PEER_PROCESS_PID_KEY = new AttributeKey<Integer>();
    private static final long KEY_FACTORY_SEED = 0x585;
    private final ChordNodeFactory node_factory;
    private final JavaProcessBuilder process_builder;
    private final SHA1KeyFactory key_factory;

    public ChordManager(JavaProcessBuilder process_builder) {

        this.process_builder = process_builder;
        node_factory = new ChordNodeFactory();
        key_factory = new SHA1KeyFactory(KEY_FACTORY_SEED);
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process node_process = process_builder.start(host, "-s:0", "-x" + nextPeerKey().toString(Key.DEFAULT_RADIX));
        final String address_as_string = ProcessUtil.scanProcessOutput(node_process, NodeServer.CHORD_NODE_LOCAL_ADDRESS_KEY, PROCESS_START_TIMEOUT);
        final String runtime_mx_bean_name = ProcessUtil.scanProcessOutput(node_process, NodeServer.RUNTIME_MX_BEAN_NAME_KEY, PROCESS_START_TIMEOUT);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final Integer pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mx_bean_name);
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), address.getPort()));
        descriptor.setAttribute(PEER_PROCESS_KEY, node_process);
        descriptor.setAttribute(PEER_PROCESS_PID_KEY, pid);
        return node_reference;
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

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            killByProcessID(descriptor);
        }
        finally {
            destroyProcess(descriptor);
        }
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
}
