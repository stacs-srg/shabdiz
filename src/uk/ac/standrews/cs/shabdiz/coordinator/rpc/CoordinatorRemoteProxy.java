package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;

/**
 * The Class ExperimentCoordinatorProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteProxy extends StreamProxy implements ICoordinatorRemote {

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
    public Marshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // IJobCoordinatorRemote overridden methods

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            getMarshaller().serializeUUID(job_id, writer);
            marshaller.serializeSerializable(result, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_EXCEPTION_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            getMarshaller().serializeUUID(job_id, writer);
            getMarshaller().serializeException(exception, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
