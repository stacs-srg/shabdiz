package uk.ac.standrews.cs.mcjob.worker;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemoteReference;
import uk.ac.standrews.cs.mcjob.worker.rpc.WorkerRemoteProxy;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Holds a reference to a remote McJob node, with a locally cached copy of its key and socket address.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteReference implements IWorkerRemoteReference {

    private final InetSocketAddress address;
    private final WorkerRemoteProxy reference;

    public WorkerRemoteReference(final InetSocketAddress address) {

        this.address = address;
        reference = WorkerRemoteProxy.getProxy(address);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public InetSocketAddress getCachedAddress() {

        return address;
    }

    @Override
    public IWorkerRemote getRemote() {

        return reference;
    }

    @Override
    public void ping() throws RPCException {

        reference.ping();
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (address == null ? 0 : address.hashCode());
        result = prime * result + (reference == null ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }

        if (!(obj instanceof WorkerRemoteReference)) { return false; }

        final WorkerRemoteReference other = (WorkerRemoteReference) obj;

        if (address == null) {
            if (other.address != null) { return false; }
        }
        else if (!address.equals(other.address)) { return false; }

        if (reference == null) {
            if (other.reference != null) { return false; }
        }
        else if (!reference.equals(other.reference)) { return false; }

        return true;
    }

    @Override
    public String toString() {

        return getRemote().toString();
    }
}
