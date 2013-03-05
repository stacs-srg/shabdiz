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
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;

/**
 * Handles communication with an {@link WorkerRemote}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class WorkerRemoteProxy extends StreamProxy implements WorkerRemote {

    /** The remote method name for {@link WorkerRemote#submit(JobRemote)}. */
    static final String SUBMIT_REMOTE_METHOD_NAME = "submitJob";

    /** The remote method name for {@link WorkerRemote#shutdown()}. */
    static final String SHUTDOWN_REMOTE_METHOD_NAME = "shutdown";

    private final ShabdizRemoteMarshaller marshaller;

    WorkerRemoteProxy(final InetSocketAddress worker_address) {

        super(worker_address);
        marshaller = new ShabdizRemoteMarshaller();
    }

    @Override
    public ShabdizRemoteMarshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public UUID submitJob(final JobRemote<? extends Serializable> job) throws RPCException {

        try {

            final AbstractStreamConnection connection = startCall(SUBMIT_REMOTE_METHOD_NAME);
            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeRemoteJob(job, writer);
            final JSONReader reader = makeCall(connection);
            final UUID job_id = Marshaller.deserializeUUID(reader);
            finishCall(connection);
            return job_id;
        }
        catch (final Exception e) {

            dealWithException(e);
            return null;
        }
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
}
