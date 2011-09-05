package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;

/**
 * The Class CoordinatedFutureRemoteReferenceWrapper.
 *
 * @param <Result> the generic type
 */
public class CoordinatedFutureRemoteReferenceWrapper<Result extends Serializable> implements IFutureRemoteReference<Result> {

    private final FutureRemoteReference<Result> future_reference;
    private final Coordinator coordinator;
    private boolean cancelled = false; // Whether this pending result was cancelled
    private volatile CountDownLatch result_available;

    /**
     * Instantiates a new coordinated future remote reference wrapper.
     *
     * @param coordinator the coordinator
     * @param future_reference the future remote reference to wrap
     */
    CoordinatedFutureRemoteReferenceWrapper(final Coordinator coordinator, final FutureRemoteReference<Result> future_reference, final CountDownLatch result_available) {

        this.coordinator = coordinator;
        this.future_reference = future_reference;
        this.result_available = result_available;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public UUID getId() {

        return future_reference.getId();
    }

    @Override
    public InetSocketAddress getAddress() {

        return future_reference.getAddress();
    }

    @Override
    public IFutureRemote<Result> getRemote() {

        final IFutureRemote<Result> real_remote = future_reference.getRemote();

        return new IFutureRemote<Result>() {

            @Override
            public boolean cancel(final boolean may_interrupt_if_running) throws RPCException {

                if (isDone()) { return false; } // Check whether the job is done; if so, cannot be cancelled

                cancelled = real_remote.cancel(may_interrupt_if_running);

                return cancelled;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Result get() throws InterruptedException, ExecutionException, RPCException {

                result_available.await(); // Wait until the result is available

                if (coordinator.notifiedCompletionsContains(future_reference)) { return (Result) coordinator.getNotifiedResult(future_reference); }

                if (coordinator.notifiedExceptionsContains(future_reference)) {
                    launchAppropreateException(coordinator.getNotifiedException(future_reference));
                }

                throw new CancellationException();
            }

            @Override
            public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException {

                result_available.await(timeout, unit);

                return get();
            }

            @Override
            public boolean isCancelled() {

                return cancelled;
            }

            @Override
            public boolean isDone() {

                return coordinator.notifiedCompletionsContains(future_reference) || coordinator.notifiedExceptionsContains(future_reference) || cancelled;
            }
        };
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void launchAppropreateException(final Exception exception) throws InterruptedException, ExecutionException, RPCException {

        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }
        if (exception instanceof RPCException) { throw (RPCException) exception; }
        if (exception instanceof RuntimeException) { throw (RuntimeException) exception; }

        throw new ExecutionException("unexpected exception was notified to the coordinator", exception);
    }
}
