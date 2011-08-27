package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteFuture;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorNode;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public final class CoordinatedRemoteFuture<Result extends Serializable> implements IRemoteFuture<Result> {

    private final UUID job_id;
    private final ICoordinatorNode coordinator;

    CoordinatedRemoteFuture(final UUID job_id, final ICoordinatorNode coordinator) {

        this.job_id = job_id;
        this.coordinator = coordinator;
    }

    @Override
    public boolean cancel(final boolean may_interrupt_if_running) throws RPCException {

        return coordinator.cancel(job_id, may_interrupt_if_running);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result get() throws CancellationException, InterruptedException, ExecutionException, RPCException {

        return (Result) coordinator.get(job_id);
    }

    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException {

        final FutureTask<Result> t = new FutureTask<Result>(new Callable<Result>() {

            @Override
            public Result call() throws Exception {

                return get();
            }
        });

        return t.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() throws RPCException {

        return coordinator.isCancelled(job_id);
    }

    @Override
    public boolean isDone() throws RPCException {

        return coordinator.isDone(job_id);
    }
}
