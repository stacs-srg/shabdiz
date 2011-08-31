package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;

/**
 * Presents the coordinator server for {@link IRemoteJob}s.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteServer extends ApplicationServer {

    /** The coordinator server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz Coordinator Server";

    private final ICoordinatorRemote application;
    private final WorkerRemoteMarshaller marshaller;

    public CoordinatorRemoteServer(final ICoordinatorRemote application) {

        super();
        this.application = application;

        marshaller = new WorkerRemoteMarshaller();
        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public Marshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public String getApplicationRegistryKey() {

        return APPLICATION_REGISTRY_KEY;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void initHandlers() {

        handler_map.put(ICoordinatorRemote.NOTIFY_COMPLETION_METHOD_NAME, new NotifyCompletionHandler());
        handler_map.put(ICoordinatorRemote.NOTIFY_EXCEPTION_REMOTE_METHOD_NAME, new NotifyExceptionHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Request Handler classes

    private final class NotifyCompletionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final UUID job_id = getMarshaller().deserializeUUID(args);
            final Serializable result = marshaller.deserializeSerializable(args);

            application.notifyCompletion(job_id, result);
            response.value("");
        }
    }

    private final class NotifyExceptionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final UUID job_id = getMarshaller().deserializeUUID(args);
            final Exception exception = getMarshaller().deserializeException(args);

            application.notifyException(job_id, exception);
            response.value("");
        }
    }
}
