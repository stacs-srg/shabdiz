package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;

/**
 * The Class McJobRemoteProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteProxy extends StreamProxy implements IWorkerRemote {

    private final WorkerRemoteMarshaller marshaller;

    private WorkerRemoteProxy(final InetSocketAddress node_address) {

        super(node_address);
        marshaller = new WorkerRemoteMarshaller();
    }

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    public static WorkerRemoteProxy getProxy(final InetSocketAddress address) {

        return null;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        try {

            final AbstractStreamConnection streams = startCall(GET_ADDRESS_METHOD_NAME);

            final JSONReader reader = makeCall(streams);
            final InetSocketAddress address = marshaller.deserializeInetSocketAddress(reader);

            finishCall(streams);

            return address;

        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public UUID submit(final IRemoteJob<?> remote_job) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(SUBMIT_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeRemoteJob(remote_job, writer);

            final JSONReader reader = makeCall(connection);
            final UUID job_id = marshaller.deserializeUUID(reader);

            finishCall(connection);

            return job_id;

        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(CANCEL_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);
            writer.value(may_interrupt_if_running);

            final JSONReader reader = makeCall(connection);
            final boolean cancelled = reader.booleanValue();

            finishCall(connection);

            return cancelled;

        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
    }

    @Override
    public boolean isCancelled(final UUID job_id) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(CANCEL_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final boolean cancelled = reader.booleanValue();

            finishCall(connection);

            return cancelled;
        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
    }

    @Override
    public boolean isDone(final UUID job_id) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(CANCEL_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final boolean done = reader.booleanValue();

            finishCall(connection);

            return done;
        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
    }

    @Override
    public Serializable get(final UUID job_id) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(CANCEL_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final Serializable result = marshaller.deserializeSerializable(reader);

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
    }

}
