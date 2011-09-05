package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManagerFactory;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.interfaces.IMadfaceManager;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteServer;
import uk.ac.standrews.cs.shabdiz.interfaces.ICoordinator;
import uk.ac.standrews.cs.shabdiz.interfaces.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * Deploys workers on a set of added hosts and provides a coordinated proxy to communicate with them.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Coordinator implements ICoordinator, ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private static final int ONE = 1;

    private final InetSocketAddress coordinator_server_address; // The address on which the coordinator server is exposed
    private final CoordinatorRemoteServer server; // The server which listens to the notifications from workers
    private final Map<IFutureRemoteReference<? extends Serializable>, Serializable> notified_completions; // Stores mapping of a remote result reference to its notified result
    private final Map<IFutureRemoteReference<? extends Serializable>, Exception> notified_exceptions; // Stores mapping of a remote result reference to its notified exception
    private final Map<IFutureRemoteReference<? extends Serializable>, CountDownLatch> future_latch_map; // Stores mapping of a remote result reference to its result availability latch

    private final IMadfaceManager madface_manager;

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new  coordinator and  starts a local server which listens to the notifications from workers on an <i>ephemeral</i> port number.
     *
     * @param application_lib_urls the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Coordinator(final Set<URL> application_lib_urls) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(EPHEMERAL_PORT, application_lib_urls);
    }

    /**
     * Instantiates a new coordinator node and starts a local server which listens to the notifications from workers on the given port number.
     *
     * @param port the port on which to start the coordinator server
     * @param application_lib_urls the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Coordinator(final int port, final Set<URL> application_lib_urls) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        notified_completions = new ConcurrentSkipListMap<IFutureRemoteReference<? extends Serializable>, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<IFutureRemoteReference<? extends Serializable>, Exception>();
        future_latch_map = new ConcurrentSkipListMap<IFutureRemoteReference<? extends Serializable>, CountDownLatch>();

        server = new CoordinatorRemoteServer(this);
        expose(NetworkUtil.getLocalIPv4InetSocketAddress(port));
        coordinator_server_address = server.getAddress();

        madface_manager = MadfaceManagerFactory.makeMadfaceManager();
        madface_manager.configureApplication(new WorkerManager());
        madface_manager.configureApplication(application_lib_urls);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized IWorkerRemote deployWorkerOnHost(final HostDescriptor host_descriptor) throws Exception {

        configureApplicationDeploymentParams(host_descriptor); // Configure the host's application deployment parameters

        madface_manager.setHostScanning(true); // Start MADFACE scanners
        madface_manager.deploy(host_descriptor); // Deploy worker on host
        madface_manager.waitForHostToReachState(host_descriptor, HostState.RUNNING); // Block until the worker is running
        madface_manager.setHostScanning(false); // Stop MADFACE scanners

        host_descriptor.shutdown(); // XXX discuss whether to shut down the process manager of host descriptor

        final WorkerRemoteProxy worker_remote_proxy = (WorkerRemoteProxy) host_descriptor.getApplicationReference(); // Retrieve the remote proxy of the deployed worker
        final IWorkerRemote coordinated_worker = wrapInCoordinatedWorker(worker_remote_proxy); // Wrap the worker's remote proxy in a coordinated version

        return coordinated_worker; // return the coordinated proxy to the worker
    }

    @Override
    public <Result extends Serializable> void notifyCompletion(final IFutureRemoteReference<Result> future_reference, final Result result) throws RPCException {

        System.out.println("Coordinator.notifyCompletion() -> future_reference: " + future_reference);
        System.out.println("Coordinator.notifyCompletion() -> result: " + result);
        notified_completions.put(future_reference, result);
        countDownLatch(future_reference);
    }

    @Override
    public <Result extends Serializable> void notifyException(final IFutureRemoteReference<Result> future_reference, final Exception exception) throws RPCException {

        notified_exceptions.put(future_reference, exception);
        countDownLatch(future_reference);
    }

    /**
     * Unexposes the coordinator Server which breaks the communication to the workers deployed by this coordinator.
     * @see ICoordinator#shutdown()
     */
    @Override
    public void shutdown() {

        unexpose();
        madface_manager.shutdown();
        // XXX discuss whether to clear out all the notifications
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Package protected methods

    boolean notifiedCompletionsContains(final FutureRemoteReference<? extends Serializable> future_reference) {

        return notified_completions.containsKey(future_reference);
    }

    boolean notifiedExceptionsContains(final FutureRemoteReference<? extends Serializable> future_reference) {

        return notified_exceptions.containsKey(future_reference);
    }

    Serializable getNotifiedResult(final FutureRemoteReference<? extends Serializable> future_reference) {

        return notified_completions.get(future_reference);
    }

    Exception getNotifiedException(final FutureRemoteReference<? extends Serializable> future_reference) {

        return notified_exceptions.get(future_reference);
    }

    CountDownLatch getResultAvailableLatch(final FutureRemoteReference<? extends Serializable> future_reference) {

        final CountDownLatch result_available = new CountDownLatch(ONE);
        future_latch_map.put(future_reference, result_available); // Associate the given future reference with a latch

        return result_available;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private <Result extends Serializable> void countDownLatch(final IFutureRemoteReference<Result> future_reference) {

        if (future_latch_map.containsKey(future_reference)) {
            future_latch_map.get(future_reference).countDown();
        }
    }

    private void configureApplicationDeploymentParams(final HostDescriptor host_descriptor) {

        final Object[] application_deployment_params = new Object[]{coordinator_server_address};
        host_descriptor.applicationDeploymentParams(application_deployment_params);
    }

    private CoordinatedWorkerWrapper wrapInCoordinatedWorker(final WorkerRemoteProxy worker_remote_proxy) {

        return new CoordinatedWorkerWrapper(this, worker_remote_proxy);
    }

    private void expose(final InetSocketAddress expose_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(expose_address.getAddress());
        server.setPort(expose_address.getPort());

        server.start(true);
    }

    private void unexpose() {

        try {
            server.stop();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "Unable to stop coordinator server, because: ", e.getMessage(), e);
        }
    }
}
