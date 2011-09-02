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
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.Worker;

/**
 * The Class McJobRemoteServer.
 */
public class WorkerRemoteServer extends ApplicationServer {

    /** The worker server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz worker server";

    private final WorkerRemoteMarshaller marshaller;

    private final Worker worker;

    /**
     * Instantiates a new worker remote server for a given worker node.
     *
     * @param worker the worker
     */
    public WorkerRemoteServer(final Worker worker) {

        super();
        this.worker = worker;
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

        handler_map.put(WorkerRemoteProxy.GET_ADDRESS_REMOTE_METHOD_NAME, new GetAddressHandler());
        handler_map.put(WorkerRemoteProxy.SUBMIT_REMOTE_METHOD_NAME, new SubmitHandler());
        handler_map.put(WorkerRemoteProxy.SHUTDOWN_REMOTE_METHOD_NAME, new ShutdownHandler());

        handler_map.put(FutureRemoteProxy.CANCEL_REMOTE_METHOD_NAME, new CancelHandler());
        handler_map.put(FutureRemoteProxy.GET_REMOTE_METHOD_NAME, new GetHandler());
        handler_map.put(FutureRemoteProxy.GET_WITH_TIMEOUT_REMOTE_METHOD_NAME, new GetWithTimeoutHandler());
        handler_map.put(FutureRemoteProxy.IS_CANCELLED_REMOTE_METHOD_NAME, new IsCancelledHandler());
        handler_map.put(FutureRemoteProxy.IS_DONE_REMOTE_METHOD_NAME, new IsDoneHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final class GetAddressHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            marshaller.serializeInetSocketAddress(worker.getAddress(), response);
        }
    }

    private final class SubmitHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final IRemoteJob<? extends Serializable> job = marshaller.deserializeRemoteJob(args);

                final FutureRemoteReference<? extends Serializable> future_remote_reference = worker.submit(job);
                marshaller.serializeFutureRemoteReference(future_remote_reference, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class ShutdownHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            worker.shutdown();
            response.value("");
        }
    }

    private final class CancelHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = marshaller.deserializeUUID(args);
                final boolean may_interrupt_if_running = args.booleanValue();

                final boolean cancelled = worker.getFutureById(job_id).cancel(may_interrupt_if_running);
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

                final Serializable result = worker.getFutureById(job_id).get();
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

                final Serializable result = worker.getFutureById(job_id).get(timeout, unit);
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

                final boolean cancelled = worker.getFutureById(job_id).isCancelled();
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

                final boolean done = worker.getFutureById(job_id).isDone();
                response.value(done);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }
}
