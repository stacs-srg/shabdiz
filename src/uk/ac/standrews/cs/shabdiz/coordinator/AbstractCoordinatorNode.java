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
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorNode;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorker;

/**
 * An abstract implementation of an {@link ICoordinatorNode}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractCoordinatorNode implements ICoordinatorNode {

    private final Map<IFutureRemote<? extends Serializable>, IWorker> future_worker_map; // Maps a remote pending result to a remote worker
    private final ConcurrentSkipListMap<IWorker, HostDescriptor> worker_host_map; // Maps a remote worker to a host descriptor; this map is used for convenient lookup of a host descriptor on which a worker has been deployed
    private final WorkerManager worker_manager; // Provides management hooks for workers
    private final IMadfaceManager madface_manager; // Handles the deployment of workers

    // -------------------------------------------------------------------------------------------------------------------------------

    protected AbstractCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        future_worker_map = new ConcurrentSkipListMap<IFutureRemote<? extends Serializable>, IWorker>();
        worker_host_map = new ConcurrentSkipListMap<IWorker, HostDescriptor>();
        worker_manager = new WorkerManager(try_registry_on_connection_error);
        madface_manager = MadfaceManagerFactory.makeMadfaceManager();

        configureMadfaceManager(application_lib_urls); // Configure madface for the deployment of worker nodes
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addHost(final HostDescriptor host_descriptor) throws LibrariesOverwrittenException {

        madface_manager.add(host_descriptor);
    }

    @Override
    public SortedSet<IWorker> deployWorkersOnHosts() throws Exception {

        madface_manager.deployAll();
        madface_manager.waitForAllToReachState(HostState.RUNNING);
        madface_manager.setHostScanning(false);

        populateWorkerHostMap(); // Populate the map of workers to host descriptors
        return getDeployedWorkersOnHosts(); // return the set of deployed workers on hosts
    }

    @Override
    public synchronized void killWorker(final IWorker worker) throws Exception {

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

    protected Set<IFutureRemote<? extends Serializable>> getFuturesByWorker(final IWorker worker) {

        final Set<IFutureRemote<? extends Serializable>> jobs_by_worker = new HashSet<IFutureRemote<? extends Serializable>>();

        for (final Entry<IFutureRemote<? extends Serializable>, IWorker> job : future_worker_map.entrySet()) {

            if (job.getValue().equals(worker)) { // Check whether the job belongs to the given worker
                jobs_by_worker.add(job.getKey());
            }
        }

        return jobs_by_worker;
    }

    protected Set<IFutureRemote<? extends Serializable>> getAllFutures() {

        return future_worker_map.keySet();
    }

    protected SortedSet<IWorker> getDeployedWorkersOnHosts() {

        return worker_host_map.keySet();
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

    private void cancelJobsByWorker(final IWorker worker) {

        final Set<IFutureRemote<? extends Serializable>> jobs_to_cancel = getFuturesByWorker(worker);
        cancelJobsSilently(jobs_to_cancel);
    }

    private void killHostByWorker(final IWorker worker) throws Exception {

        final HostDescriptor host = worker_host_map.get(worker);
        killHost(host);
    }

    private void killHost(final HostDescriptor host) throws Exception {

        madface_manager.kill(host, false);
        madface_manager.drop(host);
    }

    private void dropWorker(final IWorker worker) {

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

            final IWorker remote_reference = (IWorker) deployed_host.getApplicationReference();
            worker_host_map.put(remote_reference, deployed_host);
        }
    }
}
