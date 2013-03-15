/*
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents a proxy to the pending result of a remote computation.
 * 
 * @param <Result> the type of pending result
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class PassiveFutureRemoteProxy<Result extends Serializable> implements Future<Result> {

    private static final int LATCH_COUNT = 1; // The latch count is one because there is only one notification is needed to be received from the remote worker to release this pending result

    private enum State {

        /** Indicates that this future is pending for the notification from the remote worker. */
        PENDING,

        /** Indicates that pending has ended in a result. */
        DONE_WITH_RESULT,

        /** Indicates that pending has ended in an exception. */
        DONE_WITH_EXCEPTION,

        /** Indicates that pending has ended in cancellation of the job. */
        CANCELLED;
    }

    private final UUID job_id; // The globally unique ID of the job
    private final CountDownLatch job_done_latch; // Allows this thread to wait until the remote computation is complete

    private Exception exception; // Placeholder of the exception which is produced as the outcome of the remote job execution
    private Result result; // Placeholder of the result which is produced as the outcome of the remote job execution
    private State current_state; // Current state of this future remote

    private final WorkerRemote proxy;

    /**
     * Instantiates a new proxy to the pending result of a remote computation.
     * 
     * @param job_id the id of the remote computation
     * @param worker_address the address of the worker which performs the computation
     */
    PassiveFutureRemoteProxy(final UUID job_id, final WorkerRemote proxy) {

        this.job_id = job_id;
        this.proxy = proxy;

        current_state = State.PENDING;
        job_done_latch = new CountDownLatch(LATCH_COUNT);
    }

    @Override
    public boolean cancel(final boolean may_interrupt) {

        if (isDone()) { return false; } // Check whether the job is done; if so, cannot be cancelled

        boolean cancelled = false;
        try {
            cancelled = cancelOnRemote(may_interrupt);
        }
        catch (final JsonRpcException e) {
            setException(e); // Since unable to communicate with the remote worker, there is no point to wait for notification.
        }
        finally {
            if (cancelled) {
                updateState(State.CANCELLED);
            }
        }
        return cancelled;
    }

    @Override
    public Result get() throws InterruptedException, ExecutionException {

        job_done_latch.await(); // Wait until the job is done

        switch (current_state) {
            case DONE_WITH_RESULT:
                return result;

            case DONE_WITH_EXCEPTION:
                launchAppropreateException(exception);
                break;
            case CANCELLED:
                throw new CancellationException();

            default:
                break;
        }

        throw new IllegalStateException("The latch count is zero when the job is not done");
    }

    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        if (job_done_latch.await(timeout, unit)) { return get(); }

        throw new TimeoutException();
    }

    @Override
    public boolean isCancelled() {

        return current_state == State.CANCELLED;
    }

    @Override
    public boolean isDone() {

        return current_state != State.PENDING;
    }

    UUID getJobID() {

        return job_id;
    }

    void setException(final Exception exception) {

        this.exception = exception;
        updateState(State.DONE_WITH_EXCEPTION);
    }

    @SuppressWarnings("unchecked")
    void setResult(final Serializable result) {

        try {

            this.result = (Result) result;
            updateState(State.DONE_WITH_RESULT);
        }
        catch (final ClassCastException e) {
            setException(new ExecutionException("Unable to cast the notified result to the appropriate type", e));
        }
    }

    @Override
    public int hashCode() {

        return job_id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        final PassiveFutureRemoteProxy<?> other = (PassiveFutureRemoteProxy<?>) obj;
        if (current_state != other.current_state) { return false; }
        if (exception == null) {
            if (other.exception != null) { return false; }
        }
        else if (!exception.equals(other.exception)) { return false; }
        if (job_done_latch == null) {
            if (other.job_done_latch != null) { return false; }
        }
        else if (!job_done_latch.equals(other.job_done_latch)) { return false; }
        if (job_id == null) {
            if (other.job_id != null) { return false; }
        }
        else if (!job_id.equals(other.job_id)) {
            return false;
        }
        else if (!result.equals(other.result)) { return false; }
        return true;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private boolean cancelOnRemote(final boolean may_interrupt) throws JsonRpcException {

        return proxy.cancel(job_id, may_interrupt);
    }

    private void updateState(final State new_state) {

        current_state = new_state;

        if (current_state != State.PENDING) { // Check whether this future is no longer pending
            job_done_latch.countDown(); // Release the waiting latch
        }
    }

    private void launchAppropreateException(final Exception exception) throws InterruptedException, ExecutionException {

        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }
        if (exception instanceof RPCException) { throw new ExecutionException(exception); }
        if (exception instanceof RuntimeException) { throw (RuntimeException) exception; }

        throw new ExecutionException("unexpected exception was notified by the worker : " + exception.getClass(), exception);
    }
}
