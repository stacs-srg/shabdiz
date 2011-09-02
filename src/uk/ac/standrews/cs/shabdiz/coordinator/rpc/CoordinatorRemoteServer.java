package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.shabdiz.interfaces.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;

/**
 * Presents the coordinator server for {@link IJobRemote}s.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteServer extends ApplicationServer {

    /** The coordinator server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz Coordinator Server";

    private final ICoordinatorRemote coordinator;
    private final WorkerRemoteMarshaller marshaller;

    /**
     * Instantiates a new coordinator remote server.
     *
     * @param coordinator the coordinator application
     */
    public CoordinatorRemoteServer(final ICoordinatorRemote coordinator) {

        super();
        this.coordinator = coordinator;

        marshaller = new WorkerRemoteMarshaller();
        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public String getApplicationRegistryKey() {

        return APPLICATION_REGISTRY_KEY;
    }

    @Override
    public InetSocketAddress getAddress() {

        if (server_socket == null || port != 0) { return super.getAddress(); }

        return (InetSocketAddress) server_socket.getLocalSocketAddress();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void initHandlers() {

        handler_map.put(CoordinatorRemoteProxy.NOTIFY_COMPLETION_REMOTE_METHOD_NAME, new NotifyCompletionHandler());
        handler_map.put(CoordinatorRemoteProxy.NOTIFY_EXCEPTION_REMOTE_METHOD_NAME, new NotifyExceptionHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Request Handler classes

    private final class NotifyCompletionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final IFutureRemoteReference<Serializable> future_reference = getMarshaller().deserializeFutureRemoteReference(args);
            final Serializable result = marshaller.deserializeSerializable(args);

            coordinator.notifyCompletion(future_reference, result);
            response.value("");
        }
    }

    private final class NotifyExceptionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final IFutureRemoteReference<Serializable> future_reference = getMarshaller().deserializeFutureRemoteReference(args);
            final Exception exception = getMarshaller().deserializeException(args);

            coordinator.notifyException(future_reference, exception);
            response.value("");
        }
    }
}
