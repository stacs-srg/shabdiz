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
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerNode;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;

/**
 * Implements a passive mechanism by which a {@link IWorkerRemote} can be contacted.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class WorkerPassiveRemoteProxy extends StreamProxy implements IWorkerRemote, IWorkerNode {

    /** The remote method name for {@link IWorkerNode#submit(IJobRemote)}. */
    static final String SUBMIT_REMOTE_METHOD_NAME = "submitJob";

    /** The remote method name for {@link IWorkerNode#shutdown()}. */
    static final String SHUTDOWN_REMOTE_METHOD_NAME = "shutdown";

    private final ShabdizRemoteMarshaller marshaller;
    private final InetSocketAddress worker_address;
    private final Launcher launcher;

    /**
     * Instantiates a new coordinated worker wrapper.
     *
     * @param worker_remote the worker remote to wrap
     * @param launcher the coordinator of the remote worker
     */
    WorkerPassiveRemoteProxy(final Launcher launcher, final InetSocketAddress worker_address) {

        super(worker_address);

        this.worker_address = worker_address;
        this.launcher = launcher;
        marshaller = new ShabdizRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ShabdizRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public <Result extends Serializable> IFutureRemote<Result> submit(final IJobRemote<Result> job) throws RPCException {

        final UUID job_id = submitJob(job);

        final FutureRemoteProxy<Result> future_remote = new FutureRemoteProxy<Result>(job_id, worker_address);
        launcher.notifySubmission(future_remote);

        return future_remote;
    }

    @Override
    public void shutdown() throws RPCException {

        try {

            final AbstractStreamConnection streams = startCall(SHUTDOWN_REMOTE_METHOD_NAME);

            makeVoidCall(streams);

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public UUID submitJob(final IJobRemote<? extends Serializable> job) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(SUBMIT_REMOTE_METHOD_NAME);

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeRemoteJob(job, writer);

            final JSONReader reader = makeCall(connection);
            final UUID job_id = marshaller.deserializeUUID(reader);

            finishCall(connection);

            return job_id;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

}
