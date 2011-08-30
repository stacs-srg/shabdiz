package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.WorkerNodeImpl;

/**
 * The Class McJobRemoteServer.
 */
public class WorkerRemoteServer extends ApplicationServer {

    /** The worker server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz worker server";

    private final WorkerRemoteMarshaller marshaller;

    private final WorkerNodeImpl worker_node;

    /**
     * Instantiates a new worker remote server for a given worker node.
     */
    public WorkerRemoteServer(final WorkerNodeImpl worker_node) {

        super();
        this.worker_node = worker_node;
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

        handler_map.put(IWorkerRemote.GET_ADDRESS_METHOD_NAME, new GetAddressHandler());
        handler_map.put(IWorkerRemote.SUBMIT_METHOD_NAME, new SubmitHandler());

        handler_map.put(IFutureRemote.CANCEL_METHOD_NAME, new CancelHandler());
        handler_map.put(IFutureRemote.GET_METHOD_NAME, new GetHandler());
        handler_map.put(IFutureRemote.GET_WITH_TIMEOUT_METHOD_NAME, new GetWithTimeoutHandler());
        handler_map.put(IFutureRemote.IS_CANCELLED_METHOD_NAME, new IsCancelledHandler());
        handler_map.put(IFutureRemote.IS_DONE_METHOD_NAME, new IsDoneHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final class GetAddressHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            marshaller.serializeInetSocketAddress(worker_node.getAddress(), response);
        }
    }

    private final class SubmitHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final IRemoteJob<? extends Serializable> job = marshaller.deserializeRemoteJob(args);

                final IFutureRemoteReference<? extends Serializable> future_remote_reference = worker_node.submit(job);
                marshaller.serializeFutureRemoteReference(future_remote_reference, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class CancelHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);
                final boolean may_interrupt_if_running = args.booleanValue();

                final boolean cancelled = worker_node.getFutureById(job_id).cancel(may_interrupt_if_running);
                response.value(cancelled);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class GetHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);

                final Serializable result = worker_node.getFutureById(job_id).get();
                marshaller.serializeSerializable(result, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class GetWithTimeoutHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);
                final long timeout = args.longValue();
                final TimeUnit unit = marshaller.deserializeTimeUnit(args);

                final Serializable result = worker_node.getFutureById(job_id).get(timeout, unit);
                marshaller.serializeSerializable(result, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class IsCancelledHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);

                final boolean cancelled = worker_node.getFutureById(job_id).isCancelled();
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

                final boolean done = worker_node.getFutureById(job_id).isDone();
                response.value(done);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }
}
