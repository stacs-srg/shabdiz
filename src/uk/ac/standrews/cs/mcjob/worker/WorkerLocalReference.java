package uk.ac.standrews.cs.mcjob.worker;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerNode;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemoteReference;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * A specialised version of {@link WorkerRemoteReference}, used in testing, which retains the local reference.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class WorkerLocalReference implements IWorkerRemoteReference {

    private final IWorkerNode node;
    private final IWorkerRemoteReference remote_reference;

    public WorkerLocalReference(final IWorkerNode node, final IWorkerRemoteReference remote_reference) {

        this.node = node;
        this.remote_reference = remote_reference;
    }

    public IWorkerNode getNode() {

        return node;
    }

    @Override
    public void ping() throws RPCException {

        remote_reference.ping();
    }

    @Override
    public InetSocketAddress getCachedAddress() {

        return remote_reference.getCachedAddress();
    }

    @Override
    public IWorkerRemote getRemote() {

        return remote_reference.getRemote();
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (node == null ? 0 : node.hashCode());
        result = prime * result + (remote_reference == null ? 0 : remote_reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (!(obj instanceof WorkerLocalReference)) { return false; }

        final WorkerLocalReference other = (WorkerLocalReference) obj;

        if (node == null) {
            if (other.node != null) { return false; }
        }
        else if (!node.equals(other.node)) { return false; }

        if (remote_reference == null) {
            if (other.remote_reference != null) { return false; }
        }
        else if (!remote_reference.equals(other.remote_reference)) { return false; }

        return true;
    }

    @Override
    public String toString() {

        return remote_reference.toString();
    }

}
