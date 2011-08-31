package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * A wrapper for {@link WorkerRemoteProxy} which passively coordinate the workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatedWorkerRemoteWrapper implements IWorker {

    private final WorkerRemoteProxy worker_remote;

    /**
     * Instantiates a new coordinated worker remote proxy.
     *
     * @param worker_remote the worker_remote
     */
    CoordinatedWorkerRemoteWrapper(final WorkerRemoteProxy worker_remote) {

        this.worker_remote = worker_remote;
    }

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        return worker_remote.getAddress();
    }

    @Override
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IRemoteJob<Result> remote_job) throws RPCException {

        return null;
    }
}
