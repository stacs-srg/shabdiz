/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;

/**
 * RPC proxy to communicate with a launcher callback server.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class CallbackRemoteProxy extends StreamProxy implements WorkerCallback {

    /** The remote method name for {@link #notifyCompletion(UUID, Serializable)}. */
    static final String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(UUID, Exception)}. */
    static final String NOTIFY_EXCEPTION_REMOTE_METHOD_NAME = "notifyException";

    private final WorkerRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Package protected constructor of a new launcher callback proxy.
     * 
     * @param launcher_callback_address the address of a launcher call back server
     */
    CallbackRemoteProxy(final InetSocketAddress launcher_callback_address) {

        super(launcher_callback_address);
        marshaller = new WorkerRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            Marshaller.serializeUUID(job_id, writer);
            WorkerRemoteMarshaller.serializeSerializable(result, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_EXCEPTION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            Marshaller.serializeUUID(job_id, writer);
            WorkerRemoteMarshaller.serializeException(exception, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
