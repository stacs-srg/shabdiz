package uk.ac.standrews.cs.mcjob.coordinator;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.mcjob.coordinator.rpc.CoordinatorProxy;
import uk.ac.standrews.cs.mcjob.coordinator.rpc.CoordinatorProxyFactory;
import uk.ac.standrews.cs.mcjob.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.mcjob.interfaces.coordinator.ICoordinatorRemoteReference;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public class CoordinatorRemoteReference implements ICoordinatorRemoteReference {

    private final InetSocketAddress address;
    private final CoordinatorProxy reference;

    public CoordinatorRemoteReference(final InetSocketAddress address) {

        this.address = address;
        reference = CoordinatorProxyFactory.getProxy(address);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public InetSocketAddress getCachedAddress() {

        return address;
    }

    @Override
    public ICoordinatorRemote getRemote() {

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
        if (!(obj instanceof CoordinatorRemoteReference)) { return false; } // Checks if obj is not null and is not instance of CoordinatorRemoteReference

        final CoordinatorRemoteReference other = (CoordinatorRemoteReference) obj;
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
