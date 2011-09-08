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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
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
 * The Class CoordinatedFutureRemoteReferenceWrapper.
 *
 * @param <Result> the generic type
 */
public class FutureRemoteProxy<Result extends Serializable> extends StreamProxy implements Future<Result> {

    /** The remote method name for {@link IFutureRemote#cancel(boolean)}. */
    public static final String CANCEL_REMOTE_METHOD_NAME = "cancel";

    private static final int LATCH_COUNT = 1;

    private enum State {
        WAITING, DONE_WITH_RESULT, DONE_WITH_EXCEPTION, CANCELLED
    }

    private final UUID job_id;
    private final ShabdizRemoteMarshaller marshaller;
    private final CountDownLatch job_done_latch;

    private Exception exception;
    private Result result;
    private State current_state; // Current state of this future remote

    /**
     * Instantiates a new coordinated future remote reference wrapper.
     *
     * @param coordinator the coordinator
     * @param future_reference the future remote reference to wrap
     */
    FutureRemoteProxy(final UUID job_id, final InetSocketAddress worker_address) {

        super(worker_address);

        this.job_id = job_id;

        current_state = State.WAITING; // Set the current state to waiting
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
        catch (RPCException e) {
            setException(e);

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

        return current_state != State.WAITING;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    UUID getId() {

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
        catch (ClassCastException e) {
            setException(new ExecutionException("Unable to cast the notified result to the appropriate type", e));
        }
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

        if (current_state != State.WAITING) {
            job_done_latch.countDown();
        }
    }

    private void launchAppropreateException(final Exception exception) throws InterruptedException, ExecutionException {

        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }
        if (exception instanceof RPCException) { throw new ExecutionException(exception); }
        if (exception instanceof RuntimeException) { throw (RuntimeException) exception; }

        throw new ExecutionException("unexpected exception was notified by the launcher : " + exception.getClass(), exception);
    }
}
