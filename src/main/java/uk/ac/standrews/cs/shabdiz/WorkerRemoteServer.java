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
package uk.ac.standrews.cs.shabdiz;

import java.io.Serializable;
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;

/**
 * Handles incoming calls to a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class WorkerRemoteServer extends ApplicationServer {

    /** The worker server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz Worker Server";

    private final ShabdizRemoteMarshaller marshaller;

    private final DefaultWorkerRemote worker;

    /**
     * Instantiates a new worker remote server for a given worker node.
     * 
     * @param worker the worker
     */
    WorkerRemoteServer(final DefaultWorkerRemote worker) {

        super();
        this.worker = worker;
        marshaller = new ShabdizRemoteMarshaller();

        initRPCHandlers();
    }

    @Override
    public Marshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public String getApplicationRegistryKey() {

        return APPLICATION_REGISTRY_KEY;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void initRPCHandlers() {

        handler_map.put(WorkerRemoteProxy.SUBMIT_REMOTE_METHOD_NAME, new SubmitHandler());
        handler_map.put(WorkerRemoteProxy.SHUTDOWN_REMOTE_METHOD_NAME, new ShutdownHandler());
        handler_map.put(FutureRemoteProxy.CANCEL_REMOTE_METHOD_NAME, new CancelHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final class SubmitHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final JobRemote<? extends Serializable> job = marshaller.deserializeRemoteJob(args);

                final UUID job_id = worker.submitJob(job);
                Marshaller.serializeUUID(job_id, response);
            }
            catch (final DeserializationException e) {
                throw new RemoteWorkerException(e);
            }
        }
    }

    private final class ShutdownHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            worker.shutdown();
            response.value("");
        }
    }

    private final class CancelHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final UUID job_id = Marshaller.deserializeUUID(args);
                final boolean may_interrupt_if_running = args.booleanValue();
                final boolean cancelled = worker.cancelJob(job_id, may_interrupt_if_running);
                response.value(cancelled);

                throw new RemoteWorkerException("Unable to cancel job, worker does not know of any job with the id " + job_id);
            }
            catch (final DeserializationException e) {

                throw new RemoteWorkerException(e);
            }
        }
    }
}
