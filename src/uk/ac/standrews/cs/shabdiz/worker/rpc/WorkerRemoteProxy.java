package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;

/**
 * The Class McJobRemoteProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteProxy extends StreamProxy implements IWorkerRemote {

    /** The remote method name for {@link IWorkerRemote#submit(IJobRemote)}. */
    public static final String SUBMIT_REMOTE_METHOD_NAME = "submit";

    /** The remote method name for {@link IWorkerRemote#shutdown()}. */
    public static final String SHUTDOWN_REMOTE_METHOD_NAME = "shutdown";

    private final WorkerRemoteMarshaller marshaller;

    private final InetSocketAddress worker_cached_address;

    /**
     * Package protected constructor of a worker remote proxy.
     *
     * @param worker_address the worker address
     * @see WorkerRemoteProxyFactory#getProxy(InetSocketAddress)
     */
    WorkerRemoteProxy(final InetSocketAddress worker_address) {

        super(worker_address);

        worker_cached_address = worker_address;
        marshaller = new WorkerRemoteMarshaller();
    }

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public <Result extends Serializable> FutureRemoteReference<Result> submit(final IJobRemote<Result> remote_job) throws RPCException {

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

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
      * Gets the cached address of this worker proxy.
      *
      * @return the cached address of this worker proxy
      */
    public final InetSocketAddress getCachedAddress() {

        return worker_cached_address;
    }
}
