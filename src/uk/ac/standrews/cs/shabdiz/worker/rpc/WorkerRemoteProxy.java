package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;

/**
 * The Class McJobRemoteProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteProxy extends StreamProxy implements IWorkerRemote {

    private final WorkerRemoteMarshaller marshaller;

    WorkerRemoteProxy(final InetSocketAddress node_address) {

        super(node_address);
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
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IRemoteJob<Result> remote_job) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(SUBMIT_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeRemoteJob(remote_job, writer);

            final JSONReader reader = makeCall(connection);
            final IFutureRemoteReference<Result> future_remote_reference = marshaller.deserializeFutureRemoteReference(reader);

            finishCall(connection);

            return future_remote_reference;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }
}
