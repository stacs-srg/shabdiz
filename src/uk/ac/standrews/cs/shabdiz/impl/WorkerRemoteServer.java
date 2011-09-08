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
import java.util.UUID;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;

/**
 * Handles incoming calls to a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteServer extends ApplicationServer {

    /** The worker server registry key. */
    public static final String APPLICATION_REGISTRY_KEY = "Shabdiz Worker Server";

    private final ShabdizRemoteMarshaller marshaller;

    private final WorkerRemote worker;

    /**
     * Instantiates a new worker remote server for a given worker node.
     *
     * @param worker the worker
     */
    public WorkerRemoteServer(final WorkerRemote worker) {

        super();
        this.worker = worker;
        marshaller = new ShabdizRemoteMarshaller();

        initHandlers();
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

    private void initHandlers() {

        handler_map.put(WorkerRemoteProxy.SUBMIT_REMOTE_METHOD_NAME, new SubmitHandler());
        handler_map.put(WorkerRemoteProxy.SHUTDOWN_REMOTE_METHOD_NAME, new ShutdownHandler());

        handler_map.put(FutureRemoteProxy.CANCEL_REMOTE_METHOD_NAME, new CancelHandler());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final class SubmitHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter response) throws Exception {

            try {
                final IJobRemote<? extends Serializable> job = marshaller.deserializeRemoteJob(args);

                final UUID job_id = worker.submitJob(job);
                marshaller.serializeUUID(job_id, response);
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
                final UUID job_id = marshaller.deserializeUUID(args);
                final boolean may_interrupt_if_running = args.booleanValue();

                if (worker.submittedJobsContain(job_id)) {

                    final boolean cancelled = worker.getFutureById(job_id).cancel(may_interrupt_if_running);
                    response.value(cancelled);
                }

                throw new RemoteWorkerException("Unable to cancel job, worker does not know of any job with the id " + job_id);
            }
            catch (final DeserializationException e) {

                throw new RemoteWorkerException(e);
            }
        }
    }
}
