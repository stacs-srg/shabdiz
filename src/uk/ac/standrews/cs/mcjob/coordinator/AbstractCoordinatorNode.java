package uk.ac.standrews.cs.mcjob.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.mcjob.coordinator.rpc.CoordinatorServer;
import uk.ac.standrews.cs.mcjob.interfaces.IRemoteFuture;
import uk.ac.standrews.cs.mcjob.interfaces.IRemoteJob;
import uk.ac.standrews.cs.mcjob.interfaces.coordinator.ICoordinatorNode;
import uk.ac.standrews.cs.mcjob.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemoteReference;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManagerFactory;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.madface.interfaces.IMadfaceManager;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

/**
 * The Class JobExecutorNetwork.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractCoordinatorNode implements ICoordinatorNode, ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private final IMadfaceManager madface_manager;
    private final WorkerManager worker_manager;
    private final InetSocketAddress local_address;
    private final Map<IWorkerRemoteReference, HostDescriptor> worker_host_map;
    private final Map<UUID, IWorkerRemoteReference> job_worker_map;
    protected final Map<UUID, Serializable> notified_completions;
    protected final Map<UUID, Exception> notified_exceptions;
    protected final Set<UUID> cancelled_jobs;
    private final CoordinatorServer server;

    // -------------------------------------------------------------------------------------------------------------------------------

    public AbstractCoordinatorNode(final Set<HostDescriptor> host_descriptors, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        this(EPHEMERAL_PORT, host_descriptors, application_lib_urls, try_registry_on_connection_error);
    }

    protected AbstractCoordinatorNode(final int port, final Set<HostDescriptor> host_descriptors, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        local_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);

        worker_host_map = new ConcurrentSkipListMap<IWorkerRemoteReference, HostDescriptor>();
        job_worker_map = new ConcurrentSkipListMap<UUID, IWorkerRemoteReference>();
        cancelled_jobs = new HashSet<UUID>();
        notified_completions = new ConcurrentSkipListMap<UUID, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<UUID, Exception>();

        server = new CoordinatorServer(this);
        expose();

        worker_manager = new WorkerManager(try_registry_on_connection_error);
        madface_manager = MadfaceManagerFactory.makeMadfaceManager();
        configureMadfaceManager(host_descriptors, application_lib_urls);
        deployHosts();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RPCException {

        if (!isSubmitted(job_id)) { return false; }
        if (!isDone(job_id)) { return false; }

        final IWorkerRemote remote_worker = job_worker_map.get(job_id).getRemote();
        final boolean cancelled = remote_worker.cancel(job_id, may_interrupt_if_running);

        if (cancelled) {
            updateCancelledJobs(job_id);
        }

        return cancelled;
    }

    @Override
    public final <Result extends Serializable> IRemoteFuture<Result> submit(final IWorkerRemoteReference worker, final IRemoteJob<Result> job) throws RPCException {

        final IWorkerRemote remote = worker.getRemote();
        final UUID job_id = remote.submit(job);
        job_worker_map.put(job_id, worker);

        return new CoordinatedRemoteFuture<Result>(job_id, this);
    }

    @Override
    public Set<IWorkerRemoteReference> getNodes() {

        return worker_host_map.keySet();
    }

    @Override
    public synchronized void killWorker(final IWorkerRemoteReference worker) throws Exception {

        cancelJobsByWorker(worker);

        KillHostByWorker(worker);
        drop(worker);
    }

    @Override
    public void killAllWorkers() throws Exception {

        cancelAllJobs();
        killAllHosts();
        dropAllWorkers();
    }

    @Override
    public void shutdown() {

        madface_manager.shutdown();
        unexpose();
    }

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        notified_completions.put(job_id, result);
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        notified_exceptions.put(job_id, exception);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    protected boolean isSubmitted(final UUID job_id) {

        return job_worker_map.containsKey(job_id);
    }

    protected IWorkerRemote getRemoteWorker(final UUID job_id) {

        return job_worker_map.get(job_id).getRemote();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void cancelAllJobs() {

        final Set<UUID> jobs_to_cancel = job_worker_map.keySet();
        cancelJobsSilently(jobs_to_cancel);
    }

    private void killAllHosts() throws Exception {

        madface_manager.killAll(false); // XXX discuss the boolean flag
        madface_manager.dropAll();
    }

    private void KillHostByWorker(final IWorkerRemoteReference worker) throws Exception {

        final HostDescriptor host = worker_host_map.get(worker);
        killHost(host);
    }

    private void cancelJobsByWorker(final IWorkerRemoteReference worker) {

        final Set<UUID> jobs_to_cancel = getJobsByWorker(worker);
        cancelJobsSilently(jobs_to_cancel);
    }

    private synchronized void cancelJobsSilently(final Set<UUID> jobs_to_cancel) {

        for (final UUID job_id : jobs_to_cancel) {

            try {
                if (!isDone(job_id)) {
                    cancel(job_id, true);
                }
            }
            catch (final RPCException e) {
                // Keep Calm and Carry On
            }
        }
    }

    private Set<UUID> getJobsByWorker(final IWorkerRemoteReference worker) {

        final Set<UUID> jobs_by_worker = new HashSet<UUID>();

        for (final Entry<UUID, IWorkerRemoteReference> job : job_worker_map.entrySet()) {

            if (job.getValue().equals(worker)) { // Check whether the job belongs to worker
                jobs_by_worker.add(job.getKey());
            }
        }

        return jobs_by_worker;
    }

    private void killHost(final HostDescriptor host) throws Exception {

        madface_manager.kill(host, false);
        madface_manager.drop(host);
    }

    private void updateCancelledJobs(final UUID job_id) {

        cancelled_jobs.add(job_id);
    }

    private void expose() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(local_address.getAddress());
        server.setPort(local_address.getPort());

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

    private void drop(final IWorkerRemoteReference worker) {

        worker_host_map.remove(worker);
    }

    private void dropAllWorkers() {

        worker_host_map.clear();
    }

    private void deployHosts() throws Exception {

        madface_manager.deployAll();
        madface_manager.waitForAllToReachState(HostState.RUNNING);
        madface_manager.setHostScanning(false);

        for (final HostDescriptor deployed_host : madface_manager.getHostDescriptors()) {

            final IWorkerRemoteReference remote_reference = (IWorkerRemoteReference) deployed_host.getApplicationReference();
            worker_host_map.put(remote_reference, deployed_host);
        }
    }

    private void configureMadfaceManager(final Set<HostDescriptor> host_descriptors, final Set<URL> application_lib_urls) throws LibrariesOverwrittenException {

        madface_manager.setHostScanning(true);
        madface_manager.configureApplication(worker_manager);
        madface_manager.configureApplication(application_lib_urls);

        for (final HostDescriptor new_node_descriptor : host_descriptors) {

            final Object[] application_deployment_params = new Object[]{local_address};
            new_node_descriptor.applicationDeploymentParams(application_deployment_params);
            madface_manager.add(new_node_descriptor);
        }
    }
}
