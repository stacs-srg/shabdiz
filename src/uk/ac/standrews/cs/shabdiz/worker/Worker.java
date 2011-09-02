package uk.ac.standrews.cs.shabdiz.worker;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteProxy;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteProxyFactory;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteServer;

/**
 * An implementation of {@link IWorkerRemote}. It notifies the coordinator about the completion of the submitted jobs.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Worker implements IWorkerRemote {

    private static final int THREAD_POOL_SIZE = 10; // TODO add a parameter for it in entry point server

    private final InetSocketAddress local_address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> id_to_future_map;
    private final CoordinatorRemoteProxy coordinator_proxy;
    private final WorkerRemoteServer server;

    /**
     * Instantiates a new worker.
     *
     * @param local_address the address on which the worker is exposed
     * @param coordinator_address the coordinator address
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Worker(final InetSocketAddress local_address, final InetSocketAddress coordinator_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this.local_address = local_address;

        coordinator_proxy = makeCoordinatorProxy(coordinator_address);
        id_to_future_map = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        exexcutor_service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        server = new WorkerRemoteServer(this);
        expose();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public InetSocketAddress getAddress() {

        return local_address;
    }

    @Override
    public <Result extends Serializable> FutureRemoteReference<Result> submit(final IJobRemote<Result> job) {

        final UUID job_id = generateJobId();
        final FutureRemoteReference<Result> future_reference = new FutureRemoteReference<Result>(job_id, local_address);

        final Future<Result> real_future = exexcutor_service.submit(new Callable<Result>() {

            @Override
            public Result call() throws Exception {

                try {
                    final Result result = job.call();

                    handleCompletion(future_reference, result);

                    return result;
                }
                catch (final Exception e) {
                    handleException(future_reference, e);

                    throw e;
                }
            }
        });

        id_to_future_map.put(job_id, real_future);

        return future_reference;
    }

    @Override
    public synchronized void shutdown() {

        exexcutor_service.shutdownNow();

        if (exexcutor_service.isTerminated()) {

            try {
                unexpose();
            }
            catch (final IOException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Unable to unexpose the worker server, because: ", e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the pending result of a submitted job with the given id.
     *
     * @param job_id the job id
     * @return the pending result
     */
    public Future<? extends Serializable> getFutureById(final UUID job_id) {

        return id_to_future_map.get(job_id);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    <Result extends Serializable> void handleCompletion(final FutureRemoteReference<Result> future_reference, final Result result) {

        try {
            coordinator_proxy.notifyCompletion(future_reference, result);

            id_to_future_map.remove(future_reference);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exception
            e.printStackTrace();
        }
    }

    void handleException(final FutureRemoteReference<? extends Serializable> future_reference, final Exception exception) {

        try {
            coordinator_proxy.notifyException(future_reference, exception);

            id_to_future_map.remove(future_reference);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exception
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private CoordinatorRemoteProxy makeCoordinatorProxy(final InetSocketAddress coordinator_address) {

        return CoordinatorRemoteProxyFactory.getProxy(coordinator_address);
    }

    private void expose() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(local_address.getAddress());
        server.setPort(local_address.getPort());

        server.start(true);
    }

    private void unexpose() throws IOException {

        server.stop();
    }

    private static synchronized UUID generateJobId() {

        return UUID.randomUUID();
    }
}
