package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;

/**
 * The Class ExperimentCoordinatorProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteProxy extends StreamProxy {

    /** The remote method name for {@link #notifyCompletion(UUID, Object)}. */
    public static final String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(UUID, Exception)}. */
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
    public Marshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Notifies the coordinator about the result of a submitted job.
     * 
     * @param job_id the id of the submitted job
     * @param result the result of the completed job
     * @throws RPCException if unable to contact the correspondence
     */
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_REMOTE_METHOD_NAME);

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

    /**
     * Notifies the coordinator about the exception resulted by a submitted job.
     * 
     * @param job_id the id of the submitted job
     * @param exception the exception which occurred when trying to execute a job
     * @throws RPCException if unable to contact the correspondence
     */
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_EXCEPTION_REMOTE_METHOD_NAME);

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
