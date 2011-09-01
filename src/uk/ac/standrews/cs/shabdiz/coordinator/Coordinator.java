package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
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
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * An abstract implementation of an {@link ICoordinator}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Coordinator implements ICoordinator, ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private final Map<IFutureRemote<? extends Serializable>, IWorker> future_worker_map; // Maps a remote pending result to a remote worker
    private final ConcurrentSkipListMap<IWorker, HostDescriptor> worker_host_map; // Maps a remote worker to a host descriptor; this map is used for convenient lookup of a host descriptor on which a worker has been deployed
    private final WorkerManager worker_manager; // Provides management hooks for workers
    private final SortedSet<HostDescriptor> host_descriptors;

    private WorkerNetwork worker_network;
    private final Set<URL> application_lib_urls;
    private final InetSocketAddress coordinator_server_address;

    private final CoordinatorRemoteServer server;
    private final Map<UUID, Serializable> notified_completions;
    private final Map<UUID, Exception> notified_exceptions;

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new  coordinator and  starts a local server which listens to the notifications from workers on an <i>ephemeral</i> port number.
     *
     * @param application_lib_urls the application library URLs
     * @param try_registry_on_connection_error  whether to try to lookup a worker from registry upon connection error
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Coordinator(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(EPHEMERAL_PORT, application_lib_urls, try_registry_on_connection_error);
    }

    /**
     * Instantiates a new coordinator node and starts a local server which listens to the notifications from workers on the given port number.
     *
     * @param port the port on which to start the coordinator server
     * @param application_lib_urls the application library URLs
     * @param try_registry_on_connection_error  whether to try to lookup a worker from registry upon connection error
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Coordinator(final int port, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        coordinator_server_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);
        this.application_lib_urls = application_lib_urls;

        future_worker_map = new ConcurrentSkipListMap<IFutureRemote<? extends Serializable>, IWorker>();
        worker_host_map = new ConcurrentSkipListMap<IWorker, HostDescriptor>();
        worker_manager = new WorkerManager(try_registry_on_connection_error);

        host_descriptors = new TreeSet<HostDescriptor>();
        notified_completions = new ConcurrentSkipListMap<UUID, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<UUID, Exception>();

        server = new CoordinatorRemoteServer(this);
        expose();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        notified_completions.put(job_id, result);
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        notified_exceptions.put(job_id, exception);
    }

    @Override
    public void addHost(final HostDescriptor host_descriptor) {

        host_descriptors.add(host_descriptor);
    }

    @Override
    public SortedSet<IWorker> deployWorkersOnHosts() throws Exception {

        worker_network = new WorkerNetwork(host_descriptors, worker_manager, application_lib_urls, coordinator_server_address);

        populateWorkerHostMap(); // Populate the map of workers to host descriptors
        return getDeployedWorkersOnHosts(); // return the set of deployed workers on hosts
    }

    @Override
    public void shutdown() {

        unexpose();
        worker_network.shutdown();
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    protected SortedSet<IWorker> getDeployedWorkersOnHosts() {

        return worker_host_map.keySet();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    Set<IFutureRemote<? extends Serializable>> getFuturesByWorker(final IWorker worker) {

        final Set<IFutureRemote<? extends Serializable>> jobs_by_worker = new HashSet<IFutureRemote<? extends Serializable>>();

        for (final Entry<IFutureRemote<? extends Serializable>, IWorker> job : future_worker_map.entrySet()) {

            if (job.getValue().equals(worker)) { // Check whether the job belongs to the given worker
                jobs_by_worker.add(job.getKey());
            }
        }

        return jobs_by_worker;
    }

    Set<IFutureRemote<? extends Serializable>> getAllFutures() {

        return future_worker_map.keySet();
    }

    boolean notifiedCompletionsContains(final UUID job_id) {

        return notified_completions.containsKey(job_id);
    }

    boolean notifiedExceptionsContains(final UUID job_id) {

        return notified_exceptions.containsKey(job_id);
    }

    Serializable getNotifiedResult(final UUID job_id) {

        return notified_completions.get(job_id);
    }

    Exception getNotifiedException(final UUID job_id) {

        return notified_exceptions.get(job_id);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void populateWorkerHostMap() {

        for (final HostDescriptor deployed_host : worker_network.getNodes()) {

            final WorkerRemoteProxy real_worker = (WorkerRemoteProxy) deployed_host.getApplicationReference();
            final CoordinatedWorkerWrapper coordinated_worker = new CoordinatedWorkerWrapper(this, real_worker);

            worker_host_map.put(coordinated_worker, deployed_host);
        }
    }

    private void expose() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(coordinator_server_address.getAddress());
        server.setPort(coordinator_server_address.getPort());

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
