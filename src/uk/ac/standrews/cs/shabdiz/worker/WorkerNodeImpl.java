package uk.ac.standrews.cs.shabdiz.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteProxy;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteProxyFactory;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerNode;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;

public class WorkerNodeImpl implements IWorkerNode, IWorkerRemote {

    private static final int THREAD_POOL_SIZE = 10; // PIOTA

    private final InetSocketAddress address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> future_results;
    private final CoordinatorRemoteProxy coordinator_proxy;

    public WorkerNodeImpl(final InetSocketAddress address, final InetSocketAddress coordinator_address) {

        this.address = address;
        coordinator_proxy = makeCoordinatorProxy(coordinator_address);

        exexcutor_service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        future_results = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Shared local/remote method(s)

    @Override
    public InetSocketAddress getAddress() {

        return address;
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // locally accessible methods 

    @Override
    public SortedSet<UUID> getJobIds() {

        return future_results.navigableKeySet();
    }

    @Override
    public Future<? extends Serializable> getFutureById(final UUID job_id) {

        return future_results.get(job_id);
    }

    @Override
    public void handleCompletion(final UUID job_id, final Serializable result) {

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
    public void handleException(final UUID job_id, final Exception exception) {

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
    // Remotely accessible method(s)

    @Override
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IRemoteJob<Result> job) {

        final Future<Result> real_future = exexcutor_service.submit(job);
        final UUID job_id = generateJobId();

        future_results.put(job_id, real_future);

        return new FutureRemoteReference<Result>(job_id, address);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private CoordinatorRemoteProxy makeCoordinatorProxy(final InetSocketAddress coordinator_address) {

        return CoordinatorRemoteProxyFactory.getProxy(coordinator_address);
    }

    private UUID generateJobId() {

        return UUID.randomUUID();
    }
}
