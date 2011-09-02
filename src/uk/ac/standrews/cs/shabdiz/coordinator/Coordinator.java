package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
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
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * Implements a {@link ICoordinator}. Deploys workers on a set of added hosts and provides a coordinated proxy to communicate with them.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Coordinator implements ICoordinator, ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private final Set<URL> application_lib_urls; // URLs to application libraries needed by a worker
    private final InetSocketAddress coordinator_server_address; // The address on which the coordinator server is exposed
    private final CoordinatorRemoteServer server; // The server which listens to the notifications from workers
    private final SortedSet<HostDescriptor> host_descriptors; // Stores hosts on which workers will be deployed
    private final Map<IFutureRemoteReference<? extends Serializable>, Serializable> notified_completions; // Stores mapping of a remote result reference to its notified result
    private final Map<IFutureRemoteReference<? extends Serializable>, Exception> notified_exceptions; // Stores mapping of a remote result reference to its notified exception

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

        this.application_lib_urls = application_lib_urls;

        host_descriptors = new TreeSet<HostDescriptor>();
        notified_completions = new ConcurrentSkipListMap<IFutureRemoteReference<? extends Serializable>, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<IFutureRemoteReference<? extends Serializable>, Exception>();

        server = new CoordinatorRemoteServer(this);
        expose(NetworkUtil.getLocalIPv4InetSocketAddress(port));
        coordinator_server_address = server.getAddress();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized void addHost(final HostDescriptor host_descriptor) {

        host_descriptors.add(host_descriptor);
    }

    @Override
    public synchronized SortedSet<IWorker> deployWorkersOnHosts() throws Exception {

        final WorkerNetwork worker_network = new WorkerNetwork(host_descriptors, application_lib_urls, coordinator_server_address);
        final SortedSet<IWorker> deployed_workers_on_hosts = getDeployedWorkersOnHosts(worker_network.getNodes());

        worker_network.shutdown(); // Shut down the network used to deploy workers
        dropAllHosts(); // Clear out the hosts on which workers are deployed

        return deployed_workers_on_hosts; // return the set of deployed workers on hosts
    }

    @Override
    public <Result extends Serializable> void notifyCompletion(final IFutureRemoteReference<Result> future_reference, final Result result) throws RPCException {

        notified_completions.put(future_reference, result);
    }

    @Override
    public <Result extends Serializable> void notifyException(final IFutureRemoteReference<Result> future_reference, final Exception exception) throws RPCException {

        notified_exceptions.put(future_reference, exception);
    }

    /**
     * Unexposes the coordinator Server which breaks the communication to the workers deployed by this coordinator.
     * 
     * @see ICoordinator#shutdown()
     */
    @Override
    public void shutdown() {

        unexpose();
        // XXX discuss whether to clear out all the notifications
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    boolean notifiedCompletionsContains(final IFutureRemoteReference<? extends Serializable> future_reference) {

        return notified_completions.containsKey(future_reference);
    }

    boolean notifiedExceptionsContains(final IFutureRemoteReference<? extends Serializable> future_reference) {

        return notified_exceptions.containsKey(future_reference);
    }

    Serializable getNotifiedResult(final IFutureRemoteReference<? extends Serializable> future_reference) {

        return notified_completions.get(future_reference);
    }

    Exception getNotifiedException(final IFutureRemoteReference<? extends Serializable> future_reference) {

        return notified_exceptions.get(future_reference);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private SortedSet<IWorker> getDeployedWorkersOnHosts(final SortedSet<HostDescriptor> workers_hosts) {

        final SortedSet<IWorker> workers = new TreeSet<IWorker>();

        for (final HostDescriptor worker_host : workers_hosts) { // For each host on which a worker is deployed

            final WorkerRemoteProxy worker_remote_proxy = (WorkerRemoteProxy) worker_host.getApplicationReference(); // Retrieve the reference to the worker
            final IWorker coordinated_worker = wrapInCoordinatedWorker(worker_remote_proxy); // Wrap the worker in a coordinated implementation

            workers.add(coordinated_worker); // Add the wrapped worker to the list of workers
        }

        return workers;
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

    private void dropAllHosts() {

        host_descriptors.clear();
    }
}
