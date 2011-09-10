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

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncherCallback;

/**
 * RPC proxy to communicate with a launcher callback server.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class LauncherCallbackRemoteProxy extends StreamProxy implements ILauncherCallback {

    /** The remote method name for {@link #notifyCompletion(UUID, Serializable)}. */
    public static final String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(UUID, Exception)}. */
    public static final String NOTIFY_EXCEPTION_REMOTE_METHOD_NAME = "notifyException";

    private final ShabdizRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Package protected constructor of a new launcher callback proxy.
     *
     * @param launcher_callback_address the address of a launcher call back server
     * @see LauncherCallbackRemoteProxyFactory#getProxy(InetSocketAddress)
     */
    LauncherCallbackRemoteProxy(final InetSocketAddress launcher_callback_address) {

        super(launcher_callback_address);
        marshaller = new ShabdizRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ShabdizRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeUUID(job_id, writer);
            marshaller.serializeSerializable(result, writer);

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
            marshaller.serializeUUID(job_id, writer);
            marshaller.serializeException(exception, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
