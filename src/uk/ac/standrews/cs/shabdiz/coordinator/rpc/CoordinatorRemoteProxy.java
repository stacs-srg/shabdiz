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
package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.AbstractStreamConnection;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncherCallback;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteMarshaller;

/**
 * The Class ExperimentCoordinatorProxy.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorRemoteProxy extends StreamProxy implements ILauncherCallback {

    /** The remote method name for {@link #notifyCompletion(IFutureRemoteReference, Serializable)}. */
    public static final String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(IFutureRemoteReference, Exception)}. */
    public static final String NOTIFY_EXCEPTION_REMOTE_METHOD_NAME = "notifyException";

    private final WorkerRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Package protected constructor of a new coordinator proxy.
     *
     * @param coordinator_node_address the address of a coordinator node
     * @see CoordinatorRemoteProxyFactory#getProxy(InetSocketAddress)
     */
    CoordinatorRemoteProxy(final InetSocketAddress coordinator_node_address) {

        super(coordinator_node_address);
        marshaller = new WorkerRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public WorkerRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public <Result extends Serializable> void notifyCompletion(final IFutureRemoteReference<Result> future_reference, final Result result) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_COMPLETION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeFutureRemoteReference(future_reference, writer);
            marshaller.serializeSerializable(result, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public <Result extends Serializable> void notifyException(final IFutureRemoteReference<Result> future_reference, final Exception exception) throws RPCException {

        try {
            final AbstractStreamConnection streams = startCall(NOTIFY_EXCEPTION_REMOTE_METHOD_NAME);

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeFutureRemoteReference(future_reference, writer);
            marshaller.serializeException(exception, writer);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }
}
