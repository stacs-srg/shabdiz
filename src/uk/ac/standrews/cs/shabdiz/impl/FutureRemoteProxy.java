/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.impl;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;

/**
 * Presents a proxy to the pending result of a remote computation.
 * The communications between this class and the remote worker which executes the computation are performed passively.
 *
 * @param <Result> the type of pending result
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class FutureRemoteProxy<Result extends Serializable> extends StreamProxy implements Future<Result> {

    /** The remote method name for {@link Future#cancel(boolean)}. */
    public static final String CANCEL_REMOTE_METHOD_NAME = "cancel";

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
    private final ShabdizRemoteMarshaller marshaller; // Serialises/deserialises the exchanged communication messages
    private final CountDownLatch job_done_latch; // Allows this thread to wait until the remote computation is complete

    private Exception exception; // Placeholder of the exception which is produced as the outcome of the remote job execution
    private Result result; // Placeholder of the result which is produced as the outcome of the remote job execution
    private State current_state; // Current state of this future remote

    /**
     * Instantiates a new proxy to the pending result of a remote computation.
     *
     * @param job_id the id of the remote computation
     * @param worker_address the address of the worker which performs the computation
     */
    FutureRemoteProxy(final UUID job_id, final InetSocketAddress worker_address) {

        super(worker_address);

        this.job_id = job_id;

        current_state = State.PENDING;
        job_done_latch = new CountDownLatch(LATCH_COUNT);
        marshaller = new ShabdizRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ShabdizRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean cancel(final boolean may_interrupt_if_running) {

        if (isDone()) { return false; } // Check whether the job is done; if so, cannot be cancelled

        boolean cancelled;
        try {
            cancelled = cancelOnRemote(may_interrupt_if_running);
        }
        catch (final RPCException e) {

            setException(e); // Since unable to communicate with the remote worker, there is no point to wait for notification.
            cancelled = false;
        }

        if (cancelled) {
            updateState(State.CANCELLED);
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

    // -------------------------------------------------------------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public int hashCode() {

        final int prime = 7;
        int result = 1;
        result = prime * result + (current_state == null ? 0 : current_state.hashCode());
        result = prime * result + (exception == null ? 0 : exception.hashCode());
        result = prime * result + (job_done_latch == null ? 0 : job_done_latch.hashCode());
        result = prime * result + (job_id == null ? 0 : job_id.hashCode());
        result = prime * result + (marshaller == null ? 0 : marshaller.hashCode());
        result = prime * result + (this.result == null ? 0 : this.result.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        final FutureRemoteProxy<?> other = (FutureRemoteProxy<?>) obj;
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
        else if (!job_id.equals(other.job_id)) { return false; }
        if (marshaller == null) {
            if (other.marshaller != null) { return false; }
        }
        else if (!marshaller.equals(other.marshaller)) { return false; }
        if (result == null) {
            if (other.result != null) { return false; }
        }
        else if (!result.equals(other.result)) { return false; }
        return true;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private boolean cancelOnRemote(final boolean may_interrupt_if_running) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(CANCEL_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);
            writer.value(may_interrupt_if_running);

            final JSONReader reader = makeCall(connection);
            final boolean cancelled = reader.booleanValue();

            finishCall(connection);

            return cancelled;
        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
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
