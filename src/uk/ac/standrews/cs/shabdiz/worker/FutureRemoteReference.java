package uk.ac.standrews.cs.shabdiz.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import uk.ac.standrews.cs.shabdiz.interfaces.worker.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.FutureRemoteProxyFactory;

/**
 * An implementation of a reference to the pending result of a remote computation.
 *
 * @param <Result> the type of result returned by the remote computation
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class FutureRemoteReference<Result extends Serializable> implements IFutureRemoteReference<Result> {

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
}
