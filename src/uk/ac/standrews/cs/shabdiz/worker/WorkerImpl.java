package uk.ac.standrews.cs.shabdiz.worker;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.SortedSet;
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
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteServer;

/**
 * An implementation of {@link IWorker}. It notifies the coordinator about the completion of the submitted jobs.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerImpl implements IWorker {

    private static final int THREAD_POOL_SIZE = 10; // TODO add a parameter for it in entry point server

    private final InetSocketAddress local_address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> future_results;
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
    public WorkerImpl(final InetSocketAddress local_address, final InetSocketAddress coordinator_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this.local_address = local_address;

        coordinator_proxy = makeCoordinatorProxy(coordinator_address);
        future_results = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
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
    public <Result extends Serializable> FutureRemoteReference<Result> submit(final IRemoteJob<Result> job) {

        final UUID job_id = generateJobId();
        final Future<Result> real_future = exexcutor_service.submit(new Callable<Result>() {

            @Override
            public Result call() throws Exception {

                try {
                    final Result result = job.call();

                    handleCompletion(job_id, result);
                    return result;
                }
                catch (final Exception e) {
                    handleException(job_id, e);
                    throw e;
                }
            }
        });

        future_results.put(job_id, real_future);

        return new FutureRemoteReference<Result>(job_id, local_address);
    }

    @Override
    public synchronized void shutdown() {

        exexcutor_service.shutdownNow();

        if (exexcutor_service.isTerminated()) {

            try {
                unexpose();
            }
            catch (final IOException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Unable to un-expose the worker server, because: ", e.getMessage(), e);
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

        return future_results.get(job_id);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    SortedSet<UUID> getJobIds() {

        return future_results.navigableKeySet();
    }

    void handleCompletion(final UUID job_id, final Serializable result) {

        try {
            coordinator_proxy.notifyCompletion(job_id, result);

            future_results.remove(job_id);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exception
            e.printStackTrace();
        }
    }

    void handleException(final UUID job_id, final Exception exception) {

        try {
            coordinator_proxy.notifyException(job_id, exception);

            future_results.remove(job_id);
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

    private UUID generateJobId() {

        return UUID.randomUUID();
    }

    private void expose() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(local_address.getAddress());
        server.setPort(local_address.getPort());

        server.start(false);
    }

    private void unexpose() throws IOException {

        server.stop();
    }
}
