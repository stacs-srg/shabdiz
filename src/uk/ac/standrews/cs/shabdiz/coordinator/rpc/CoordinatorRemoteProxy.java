package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;

/**
 * The Class ExperimentCoordinatorProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteProxy extends StreamProxy implements ICoordinatorRemote {

    /** The remote method name for {@link #notifyCompletion(IFutureRemoteReference, Serializable)}. */
    public static final String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(IFutureRemoteReference, Exception)}. */
    public static final String NOTIFY_EXCEPTION_REMOTE_METHOD_NAME = "notifyException";

    private final WorkerRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Package protected constructor of a new coordinator proxy.
     *
     * @param coordinator_node_address the address of a coordinator node
     * @see CoordinatorRemoteProxyFactory#getProxy(InetSocketAddress)
     */
    CoordinatorRemoteProxy(final InetSocketAddress coordinator_node_address) {

        super(coordinator_node_address);
        marshaller = new WorkerRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public <Result extends Serializable> void notifyCompletion(final IFutureRemoteReference<Result> future_reference, final Result result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeFutureRemoteReference(future_reference, writer);
            marshaller.serializeSerializable(result, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public <Result extends Serializable> void notifyException(final IFutureRemoteReference<Result> future_reference, final Exception exception) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_EXCEPTION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeFutureRemoteReference(future_reference, writer);
            marshaller.serializeException(exception, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
