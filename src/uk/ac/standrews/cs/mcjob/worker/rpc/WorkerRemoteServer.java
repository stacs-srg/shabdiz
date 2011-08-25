package uk.ac.standrews.cs.mcjob.worker.rpc;

import java.io.Serializable;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.mcjob.interfaces.IRemoteJob;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.mcjob.worker.WorkerNodeImpl;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;

/**
 * The Class McJobRemoteServer.
 */
public class WorkerRemoteServer extends ApplicationServer {

    /** The McJob worker Remote Server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "McJob worker node server";

    private final WorkerRemoteMarshaller marshaller;

    private final WorkerNodeImpl worker_node;

    /**
     * Instantiates a new McJob worker remote server.
     */
    public WorkerRemoteServer(final WorkerNodeImpl node) {

        super();
        worker_node = node;
        marshaller = new WorkerRemoteMarshaller();

        initHandlers();
    }

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

        handler_map.put(IWorkerRemote.CANCEL_METHOD_NAME, new CancelHandler());
        handler_map.put(IWorkerRemote.GET_ADDRESS_METHOD_NAME, new GetAddressHandler());
        handler_map.put(IWorkerRemote.IS_CANCELLED_METHOD_NAME, new IsCancelledHandler());
        handler_map.put(IWorkerRemote.IS_DONE_METHOD_NAME, new IsDoneHandler());
        handler_map.put(IWorkerRemote.SUBMIT_METHOD_NAME, new SubmitHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final class CancelHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);
                final boolean may_interrupt_if_running = args.booleanValue();

                final boolean cancelled = worker_node.cancel(job_id, may_interrupt_if_running);
                response.value(cancelled);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class GetAddressHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            marshaller.serializeInetSocketAddress(worker_node.getAddress(), response);
        }
    }

    private final class IsCancelledHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);

                final boolean cancelled = worker_node.isCancelled(job_id);
                response.value(cancelled);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class IsDoneHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);

                final boolean done = worker_node.isDone(job_id);
                response.value(done);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class SubmitHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final IRemoteJob<? extends Serializable> job = marshaller.deserializeRemoteJob(args);

                final UUID job_id = worker_node.submit(job);
                marshaller.serializeUUID(job_id, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }
}
