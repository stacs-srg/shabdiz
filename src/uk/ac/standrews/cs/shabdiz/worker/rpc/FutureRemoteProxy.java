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
package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;

/**
 * Handles future remote RPC mechanism.
 *
 * @param <Result> the type of pending result
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class FutureRemoteProxy<Result extends Serializable> extends StreamProxy implements IFutureRemote<Result> {

    /** The remote method name for {@link IFutureRemote#cancel(boolean)}. */
    public static final String CANCEL_REMOTE_METHOD_NAME = "cancel";

    /** The remote method name for {@link IFutureRemote#get()}. */
    public static final String GET_REMOTE_METHOD_NAME = "get";

    /** The remote method name for {@link IFutureRemote#get(long, TimeUnit)}. */
    public static final String GET_WITH_TIMEOUT_REMOTE_METHOD_NAME = "getWithTimeout";

    /** The remote method name for {@link #isCancelled()}. */
    public static final String IS_CANCELLED_REMOTE_METHOD_NAME = "isCancelled";

    /** The remote method name for {@link IFutureRemote#isDone()}. */
    public static final String IS_DONE_REMOTE_METHOD_NAME = "isDone";

    private final WorkerRemoteMarshaller marshaller;
    private final UUID job_id;

    FutureRemoteProxy(final UUID job_id, final InetSocketAddress node_address) {

        super(node_address);

        this.job_id = job_id;
        marshaller = new WorkerRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean cancel(final boolean may_interrupt_if_running) throws RPCException {

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

    @Override
    public Result get() throws InterruptedException, ExecutionException, RPCException {

        try {

            final AbstractStreamConnection connection = startCall(GET_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final Result result = marshaller.<Result>deserializeResult(reader);

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException {

        try {

            final AbstractStreamConnection connection = startCall(GET_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);
            writer.value(timeout);
            marshaller.serializeTimeUnit(unit, writer);

            final JSONReader reader = makeCall(connection);
            final Result result = marshaller.<Result>deserializeResult(reader);

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public boolean isCancelled() throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(IS_CANCELLED_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

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

    @Override
    public boolean isDone() throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(IS_DONE_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);

            final JSONReader reader = makeCall(connection);
            final boolean done = reader.booleanValue();

            finishCall(connection);

            return done;
        }
        catch (final Exception e) {
            dealWithException(e);
            return false;
        }
    }

}
