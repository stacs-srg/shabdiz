package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListMap;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManagerFactory;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.madface.interfaces.IMadfaceManager;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorNode;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;

/**
 * An abstract implementation of an {@link ICoordinatorNode}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractCoordinatorNode implements ICoordinatorNode {

    private final Map<IFutureRemote<? extends Serializable>, IWorkerRemote> future_worker_map; // Maps a remote pending result to a remote worker
    private final ConcurrentSkipListMap<IWorkerRemote, HostDescriptor> worker_host_map; // Maps a remote worker to a host descriptor; this map is used for convenient lookup of a host descriptor on which a worker has been deployed
    private final WorkerManager worker_manager; // Provides management hooks for workers
    private final IMadfaceManager madface_manager; // Handles the deployment of workers

    private boolean deployed = false; // Whether #deployHosts() is called on this coordinator

    // -------------------------------------------------------------------------------------------------------------------------------

    protected AbstractCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        future_worker_map = new ConcurrentSkipListMap<IFutureRemote<? extends Serializable>, IWorkerRemote>();
        worker_host_map = new ConcurrentSkipListMap<IWorkerRemote, HostDescriptor>();
        worker_manager = new WorkerManager(try_registry_on_connection_error);
        madface_manager = MadfaceManagerFactory.makeMadfaceManager();

        configureMadfaceManager(application_lib_urls); // Configure madface for the deployment of worker nodes
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    protected abstract <Result extends Serializable> IFutureRemote<Result> getFutureRemote(final IFutureRemoteReference<Result> future_remote_reference);

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addHost(final HostDescriptor host_descriptor) throws LibrariesOverwrittenException, AlreadyDeployedException {

        if (deployed) { throw new AlreadyDeployedException("Cannot add anymore host once deployHosts() is called on this coordinator."); } // Check whether this coordinator has already deployed the hosts

        madface_manager.add(host_descriptor);
    }

    @Override
    public void deployHosts() throws Exception {

        if (deployed) { throw new AlreadyDeployedException(); } // Check whether this coordinator has already deployed the hosts

        madface_manager.deployAll();
        madface_manager.waitForAllToReachState(HostState.RUNNING);
        madface_manager.setHostScanning(false);

        populateWorkerHostMap(); // Populate the map of workers to host descriptors
        deployed = true; // Set the deployed flag to true
    }

    @Override
    public <Result extends Serializable> IFutureRemote<Result> submit(final IWorkerRemote worker, final IRemoteJob<Result> job) throws RPCException {

        final IFutureRemoteReference<Result> future_remote_reference = worker.submit(job); // Submit the job to the remote worker and retrieve the reference to the remote pending result
        final IFutureRemote<Result> future_remote = getFutureRemote(future_remote_reference); // Get the wrapper for the remote pending result
        future_worker_map.put(future_remote, worker); // Remember the pending result of the worker

        return future_remote;
    }

    @Override
    public SortedSet<IWorkerRemote> getWorkers() {

        return worker_host_map.keySet();
    }

    @Override
    public synchronized void killWorker(final IWorkerRemote worker) throws Exception {

        cancelJobsByWorker(worker); // Cancel the jobs submitted to the worker
        killHostByWorker(worker); // Kill the host on which the worker is running on
        dropWorker(worker); // Forget about the worker
    }

    @Override
    public void killAllWorkers() throws Exception {

        cancelAllJobs(); // Cancel all the jobs
        killAllHosts(); // Kill all the hosts that run the workers
        dropAllWorkers(); // Forget about all the workers
    }

    @Override
    public void shutdown() {

        madface_manager.shutdown();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    protected Set<IFutureRemote<? extends Serializable>> getFuturesByWorker(final IWorkerRemote worker) {

        final Set<IFutureRemote<? extends Serializable>> jobs_by_worker = new HashSet<IFutureRemote<? extends Serializable>>();

        for (final Entry<IFutureRemote<? extends Serializable>, IWorkerRemote> job : future_worker_map.entrySet()) {

            if (job.getValue().equals(worker)) { // Check whether the job belongs to the given worker
                jobs_by_worker.add(job.getKey());
            }
        }

        return jobs_by_worker;
    }

    protected Set<IFutureRemote<? extends Serializable>> getAllFutures() {

        return future_worker_map.keySet();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void configureMadfaceManager(final Set<URL> application_lib_urls) throws LibrariesOverwrittenException {

        madface_manager.setHostScanning(true);
        madface_manager.configureApplication(worker_manager);
        madface_manager.configureApplication(application_lib_urls);

    }

    private void cancelAllJobs() {

        final Set<IFutureRemote<? extends Serializable>> jobs_to_cancel = getAllFutures();
        cancelJobsSilently(jobs_to_cancel);
    }

    private void killAllHosts() throws Exception {

        madface_manager.killAll(false);
        madface_manager.dropAll();
    }

    private void dropAllWorkers() {

        worker_host_map.clear();
    }

    private void cancelJobsByWorker(final IWorkerRemote worker) {

        final Set<IFutureRemote<? extends Serializable>> jobs_to_cancel = getFuturesByWorker(worker);
        cancelJobsSilently(jobs_to_cancel);
    }

    private void killHostByWorker(final IWorkerRemote worker) throws Exception {

        final HostDescriptor host = worker_host_map.get(worker);
        killHost(host);
    }

    private void killHost(final HostDescriptor host) throws Exception {

        madface_manager.kill(host, false);
        madface_manager.drop(host);
    }

    private void dropWorker(final IWorkerRemote worker) {

        worker_host_map.remove(worker);
    }

    private synchronized void cancelJobsSilently(final Set<IFutureRemote<? extends Serializable>> jobs_to_cancel) {

        for (final IFutureRemote<? extends Serializable> pending_result : jobs_to_cancel) {

            try {
                if (!pending_result.isDone()) {
                    pending_result.cancel(true);
                }
            }
            catch (final RPCException e) {
                // Keep Calm and Carry On
            }
        }
    }

    private void populateWorkerHostMap() {

        for (final HostDescriptor deployed_host : madface_manager.getHostDescriptors()) {

            final IWorkerRemote remote_reference = (IWorkerRemote) deployed_host.getApplicationReference();
            worker_host_map.put(remote_reference, deployed_host);
        }
    }
}
