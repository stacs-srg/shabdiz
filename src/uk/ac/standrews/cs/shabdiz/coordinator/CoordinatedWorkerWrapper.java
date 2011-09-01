package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * A wrapper for {@link WorkerRemoteProxy} which passively coordinate the workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatedWorkerWrapper implements IWorker {

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
    public InetSocketAddress getAddress() throws RPCException {

        return worker_remote.getAddress();
    }

    @Override
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IRemoteJob<Result> remote_job) throws RPCException {

        final FutureRemoteReference<Result> real_future_remote = worker_remote.submit(remote_job);

        return new CoordinatedFutureRemoteReferenceWrapper<Result>(coordinator, real_future_remote);
    }

    @Override
    public void shutdown() throws RPCException {

        worker_remote.shutdown();
    }
}
