package uk.ac.standrews.cs.mcjob.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.mcjob.coordinator.rpc.CoordinatorProxy;
import uk.ac.standrews.cs.mcjob.coordinator.rpc.CoordinatorProxyFactory;
import uk.ac.standrews.cs.mcjob.interfaces.IRemoteJob;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerNode;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.mcjob.worker.rpc.RemoteWorkerException;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public class WorkerNodeImpl implements IWorkerNode, IWorkerRemote {

    private static final int THREAD_POOL_SIZE = 10; // PIOTA

    private final InetSocketAddress address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> future_results;
    private final CoordinatorProxy coordinator_proxy;

    public WorkerNodeImpl(final InetSocketAddress address, final InetSocketAddress coordinator_address) throws UnreachableCoordinatorException {

        this.address = address;
        coordinator_proxy = makeCoordinatorProxy(coordinator_address);

        exexcutor_service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        future_results = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // locally accessible methods 

    @Override
    public SortedSet<UUID> getJobIds() {

        return future_results.keySet();
    }

    @Override
    public Future<? extends Serializable> getFutureById(final UUID job_id) {

        return future_results.get(job_id);
    }

    @Override
    public void handleJobCompletion(final UUID job_id, final Serializable result) {

        try {
            coordinator_proxy.notifyCompletion(job_id, result);

            future_results.remove(job_id);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exceptin
            e.printStackTrace();

        }
    }

    @Override
    public void handleJobException(final UUID job_id, final Exception exception) {

        try {
            coordinator_proxy.notifyException(job_id, exception);

            future_results.remove(job_id);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exceptin
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Remotely accessible methods

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        return address;
    }

    @Override
    public UUID submit(final IRemoteJob<? extends Serializable> job) {

        final Future<? extends Serializable> future_result = exexcutor_service.submit(job);
        final UUID job_id = generateJobId();

        future_results.put(job_id, future_result);

        return job_id;
    }

    @Override
    public boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RemoteWorkerException {

        return getFutureByIdSafely(job_id).cancel(may_interrupt_if_running);
    }

    @Override
    public boolean isCancelled(final UUID job_id) throws RemoteWorkerException {

        return getFutureByIdSafely(job_id).isCancelled();
    }

    @Override
    public boolean isDone(final UUID job_id) throws RemoteWorkerException {

        return getFutureByIdSafely(job_id).isDone();
    }

    @Override
    public Serializable get(final UUID job_id) throws RemoteWorkerException {

        try {

            return getFutureByIdSafely(job_id).get();
        }
        catch (final InterruptedException e) {

            throw new RemoteWorkerException(e);
        }
        catch (final ExecutionException e) {

            throw new RemoteWorkerException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private CoordinatorProxy makeCoordinatorProxy(final InetSocketAddress coordinator_address) throws UnreachableCoordinatorException {

        final CoordinatorProxy proxy = CoordinatorProxyFactory.getProxy(coordinator_address);
        try {
            proxy.ping(); // XXX ask Al and Graham whether this is a good practice. Counter argument: coordinator may become unreachable at any moment anyway; what's the point of pinging it
        }
        catch (final RPCException e) {
            throw new UnreachableCoordinatorException(e);
        }

        return proxy;
    }

    private Future<? extends Serializable> getFutureByIdSafely(final UUID job_id) throws RemoteWorkerException {

        final Future<? extends Serializable> future = future_results.get(job_id);

        if (future == null) { throw new RemoteWorkerException("No job found by id " + job_id); }

        return future;
    }

    private UUID generateJobId() {

        return UUID.randomUUID();
    }
}
