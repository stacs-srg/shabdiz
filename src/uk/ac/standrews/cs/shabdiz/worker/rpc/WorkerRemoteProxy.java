package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;

/**
 * The Class McJobRemoteProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteProxy extends StreamProxy implements IWorker {

    /** The remote method name for {@link IWorker#getAddress()}. */
    public static final String GET_ADDRESS_REMOTE_METHOD_NAME = "getAddress";

    /** The remote method name for {@link IWorker#submit(IRemoteJob)}. */
    public static final String SUBMIT_REMOTE_METHOD_NAME = "submit";

    /** The remote method name for {@link IWorker#shutdown()}. */
    public static final String SHUTDOWN_REMOTE_METHOD_NAME = "shutdown";

    private final WorkerRemoteMarshaller marshaller;

    /**
     * Package protected constructor of a worker remote proxy.
     *
     * @param worker_address the worker address
     * @see WorkerRemoteProxyFactory#getProxy(InetSocketAddress)
     */
    WorkerRemoteProxy(final InetSocketAddress worker_address) {

        super(worker_address);
        marshaller = new WorkerRemoteMarshaller();
    }

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        try {

            final AbstractStreamConnection streams = startCall(GET_ADDRESS_REMOTE_METHOD_NAME);

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
    public <Result extends Serializable> FutureRemoteReference<Result> submit(final IRemoteJob<Result> remote_job) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(SUBMIT_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeRemoteJob(remote_job, writer);

            final JSONReader reader = makeCall(connection);
            final FutureRemoteReference<Result> future_remote_reference = marshaller.deserializeFutureRemoteReference(reader);

            finishCall(connection);

            return future_remote_reference;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public void shutdown() throws RPCException {

        try {

            final AbstractStreamConnection streams = startCall(SHUTDOWN_REMOTE_METHOD_NAME);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
