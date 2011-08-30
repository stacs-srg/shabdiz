package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;

public class FutureRemoteProxy<Result extends Serializable> extends StreamProxy implements IFutureRemote<Result> {

    private final WorkerRemoteMarshaller marshaller;
    private final UUID job_id;
    private final InetSocketAddress node_address;

    FutureRemoteProxy(final UUID job_id, final InetSocketAddress node_address) {

        super(node_address);

        this.job_id = job_id;
        this.node_address = node_address;

        marshaller = new WorkerRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean cancel(final boolean may_interrupt_if_running) throws RPCException {

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
    public Result get() throws InterruptedException, ExecutionException, RPCException {

        try {

            final AbstractStreamConnection connection = startCall(GET_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final Result result = marshaller.deserializeResult(reader);

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException {

        try {

            final AbstractStreamConnection connection = startCall(GET_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);
            writer.value(timeout);
            marshaller.serializeTimeUnit(unit, writer);

            final JSONReader reader = makeCall(connection);
            final Result result = marshaller.deserializeResult(reader);

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public boolean isCancelled() throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(IS_CANCELLED_METHOD_NAME);

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
    public boolean isDone() throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(IS_DONE_METHOD_NAME);

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
}
