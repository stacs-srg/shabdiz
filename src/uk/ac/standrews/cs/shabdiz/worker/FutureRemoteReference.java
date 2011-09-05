package uk.ac.standrews.cs.shabdiz.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.FutureRemoteProxyFactory;

/**
 * An implementation of a reference to the pending result of a remote computation.
 *
 * @param <Result> the type of result returned by the remote computation
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class FutureRemoteReference<Result extends Serializable> implements IFutureRemoteReference<Result>, Comparable<FutureRemoteReference<Result>> {

    private final UUID id;
    private final InetSocketAddress address;
    private final IFutureRemote<Result> remote;

    /**
     * Instantiates a new reference to the pending result of a remote computation with the given <code>id</code> on the given <code>address</code>.
     *
     * @param id the id of the remote computation
     * @param address the address of the worker that executes the computation
     */
    public FutureRemoteReference(final UUID id, final InetSocketAddress address) {

        this.id = id;
        this.address = address;

        remote = FutureRemoteProxyFactory.getProxy(id, address);
    }

    @Override
    public UUID getId() {

        return id;
    }

    @Override
    public InetSocketAddress getAddress() {

        return address;
    }

    @Override
    public IFutureRemote<Result> getRemote() {

        return remote;
    }

    @Override
    public int compareTo(final FutureRemoteReference<Result> other) {

        return id.compareTo(other.id);
    }

    @Override
    public int hashCode() {

        return (id == null ? 0 : id.hashCode()) + (address == null ? 0 : address.hashCode());
    }

    @Override
    public boolean equals(final Object object) {

        if (this == object) { return true; }
        if (object == null) { return false; }
        if (getClass() != object.getClass()) { return false; }

        @SuppressWarnings("rawtypes")
        final FutureRemoteReference other = (FutureRemoteReference) object;
        if (address == null) {
            if (other.address != null) { return false; }
        }
        else if (!address.equals(other.address)) { return false; }
        if (id == null) {
            if (other.id != null) { return false; }
        }
        else if (!id.equals(other.id)) { return false; }

        return true;
    }

}