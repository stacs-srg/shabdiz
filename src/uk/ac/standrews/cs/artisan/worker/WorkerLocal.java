package uk.ac.standrews.cs.artisan.worker;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.artisan.interfaces.worker.IWorkerNode;
import uk.ac.standrews.cs.artisan.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * A specialised version of {@link WorkerRemoteReference}, used in testing, which retains the local reference.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class WorkerLocal implements IWorkerRemote {

    private final IWorkerNode node;
    private final IWorkerRemote remote;

    public WorkerLocal(final IWorkerNode node, final IWorkerRemote remote_reference) {

        this.node = node;
        remote = remote_reference;
    }

    public IWorkerNode getNode() {

        return node;
    }

    @Override
    public void ping() throws RPCException {

        remote.ping();
    }

    @Override
    public InetSocketAddress getCachedAddress() {

        return remote.getCachedAddress();
    }

}
