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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NamingThreadFactory;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;

/**
 * An implementation of {@link DefaultWorkerRemote} which notifies the launcher about the completion of the submitted jobs on a given callback address.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class DefaultWorkerRemote implements WorkerRemote {

    private final InetSocketAddress local_address;
    private final ExecutorService exexcutor;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> submitted_jobs;
    private final WorkerRemoteServer server;
    private final InetSocketAddress callback_address;
    private final CallbackRemoteProxy callback_proxy;

    private static final Logger LOGGER = Logger.getLogger(DefaultWorkerRemote.class.getName());

    DefaultWorkerRemote(final InetSocketAddress local_address, final InetSocketAddress launcher_callback_address) throws IOException {

        this.callback_address = launcher_callback_address;
        callback_proxy = new CallbackRemoteProxy(callback_address);
        submitted_jobs = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        server = new WorkerRemoteServer(this);
        expose(local_address);
        this.local_address = server.getAddress();
        exexcutor = createExecutorService();
    }

    @Override
    public UUID submitJob(final JobRemote<? extends Serializable> job) {

        final UUID job_id = generateJobId();
        exexcutor.execute(new Runnable() {

            @Override
            public void run() {

                final Future<? extends Serializable> real_future = exexcutor.submit(job);
                submitted_jobs.put(job_id, real_future);

                try {
                    //FIXME this blocks until the job is complete; Could be written nicer so that it runs after completion.
                    handleCompletion(job_id, real_future.get());
                }
                catch (final Exception e) {
                    handleException(job_id, e);
                }
            }
        });

        return job_id;
    }

    @Override
    public synchronized void shutdown() {

        exexcutor.shutdownNow();

        try {
            unexpose();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.NONE, "Unable to unexpose the worker server, because: ", e.getMessage(), e);
        }

        StreamProxy.CONNECTION_POOL.shutdown();
    }

    boolean cancelJob(final UUID job_id, final boolean may_interrupt_if_running) throws RemoteWorkerException {

        if (submitted_jobs.containsKey(job_id)) {
            final boolean cancelled = submitted_jobs.get(job_id).cancel(may_interrupt_if_running);
            if (cancelled) {
                submitted_jobs.remove(job_id);
            }
            return cancelled;
        }

        throw new RemoteWorkerException("Unable to cancel job, worker does not know of any job with the id " + job_id);
    }

    private ExecutorService createExecutorService() {

        return Executors.newCachedThreadPool(new NamingThreadFactory("worker_on_" + local_address.getPort() + "_"));
    }

    private void handleCompletion(final UUID job_id, final Serializable result) {

        try {
            callback_proxy.notifyCompletion(job_id, result);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the launcher callback rpc exception
            LOGGER.log(Level.SEVERE, "failed to notify job completion with the ID " + job_id, e);
        }
    }

    private void handleException(final UUID job_id, final Exception exception) {

        try {
            callback_proxy.notifyException(job_id, exception);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            // TODO use some sort of error manager  which handles the launcher callback rpc exception
            LOGGER.log(Level.SEVERE, "failed to notify job exception with the ID " + job_id, e);
        }
    }

    private void expose(final InetSocketAddress server_address) throws IOException {

        server.setLocalAddress(server_address.getAddress());
        server.setPort(server_address.getPort());
        server.startWithNoRegistry();
    }

    private void unexpose() throws IOException {

        server.stop();
    }

    private static synchronized UUID generateJobId() {

        return UUID.randomUUID();
    }

    public InetSocketAddress getAddress() {

        return local_address;
    }
}
