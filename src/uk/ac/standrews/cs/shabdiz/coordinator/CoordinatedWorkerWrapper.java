package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * A wrapper for {@link WorkerRemoteProxy} which passively coordinate the workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatedWorkerWrapper implements IWorkerRemote, Comparable<CoordinatedWorkerWrapper> {

    private final WorkerRemoteProxy worker_remote;
    private final Coordinator coordinator;

    /**
     * Instantiates a new coordinated worker wrapper.
     *
     * @param worker_remote the worker remote to wrap
     * @param coordinator the coordinator of the remote worker
     */
    CoordinatedWorkerWrapper(final Coordinator coordinator, final WorkerRemoteProxy worker_remote) {

        this.worker_remote = worker_remote;
        this.coordinator = coordinator;
    }

    @Override
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IJobRemote<Result> remote_job) throws RPCException {

        final FutureRemoteReference<Result> real_future_remote = worker_remote.submit(remote_job);
        final CountDownLatch result_available = coordinator.getResultAvailableLatch(real_future_remote);

        return new CoordinatedFutureRemoteReferenceWrapper<Result>(coordinator, real_future_remote, result_available);

    }

    @Override
    public void shutdown() throws RPCException {

        worker_remote.shutdown();
    }

    @Override
    public int compareTo(final CoordinatedWorkerWrapper other) {

        if (equals(other)) { return 0; }

        final int compare_cached_addresses = worker_remote.getCachedAddress().toString().compareTo(other.worker_remote.getCachedAddress().toString());
        return compare_cached_addresses;
    }
}
