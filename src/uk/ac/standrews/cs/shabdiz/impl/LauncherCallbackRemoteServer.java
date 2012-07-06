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

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncherCallback;

/**
 * Serves the incoming callback notifications from workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class LauncherCallbackRemoteServer extends ApplicationServer {

    /** The launcher callback server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Launcher Callback Server";

    private final ILauncherCallback launcher_callback;
    private final ShabdizRemoteMarshaller marshaller;

    /**
     * Instantiates a new launcher callback server.
     *
     * @param launcher_callback the launcher callback
     */
    public LauncherCallbackRemoteServer(final ILauncherCallback launcher_callback) {

        super();
        this.launcher_callback = launcher_callback;

        marshaller = new ShabdizRemoteMarshaller();
        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ShabdizRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public String getApplicationRegistryKey() {

        return APPLICATION_REGISTRY_KEY;
    }

    @Override
    public InetSocketAddress getAddress() {

        if (server_socket == null || port != 0) { return super.getAddress(); }

        return (InetSocketAddress) server_socket.getLocalSocketAddress();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void initHandlers() {

        handler_map.put(LauncherCallbackRemoteProxy.NOTIFY_COMPLETION_REMOTE_METHOD_NAME, new NotifyCompletionHandler());
        handler_map.put(LauncherCallbackRemoteProxy.NOTIFY_EXCEPTION_REMOTE_METHOD_NAME, new NotifyExceptionHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------
    // Request Handler classes

    private final class NotifyCompletionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final UUID job_id = getMarshaller().deserializeUUID(args);
            final Serializable result = ShabdizRemoteMarshaller.deserializeSerializable(args);

            launcher_callback.notifyCompletion(job_id, result);
            response.value("");
        }
    }

    private final class NotifyExceptionHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            final UUID job_id = getMarshaller().deserializeUUID(args);
            final Exception exception = getMarshaller().deserializeException(args);

            launcher_callback.notifyException(job_id, exception);
            response.value("");
        }
    }
}
