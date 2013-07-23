/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.job;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.lean.LeanClientFactory;
import org.mashti.jetson.lean.LeanServerFactory;
import org.mashti.jetson.util.NamingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DefaultWorkerRemote} which notifies the launcher about the completion of the submitted jobs on a given callback address.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultWorkerRemote implements WorkerRemote {

    private static final ServerFactory<WorkerRemote> SERVER_FACTORY = new LeanServerFactory<WorkerRemote>(WorkerRemote.class);
    private static final ClientFactory<WorkerCallback> CLIENT_FACTORY = new LeanClientFactory<WorkerCallback>(WorkerCallback.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkerRemote.class);
    private final InetSocketAddress local_address;
    private final ListeningExecutorService executor;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> submitted_jobs;
    private final Server server;
    private final WorkerCallback callback;

    protected DefaultWorkerRemote(final InetSocketAddress local_address, final InetSocketAddress callback_address) throws IOException {

        callback = CLIENT_FACTORY.get(callback_address);
        submitted_jobs = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        executor = createExecutorService();
        server = SERVER_FACTORY.createServer(this);
        server.setBindAddress(local_address);
        expose();
        this.local_address = server.getLocalSocketAddress();
    }

    @Override
    public UUID submitJob(final Job<? extends Serializable> job) {

        final UUID job_id = generateJobId();
        final ListenableFuture<? extends Serializable> future = executor.submit(job);
        final FutureCallbackNotifier future_callback = new FutureCallbackNotifier(job_id);

        Futures.addCallback(future, future_callback, executor);
        return job_id;
    }

    @Override
    public boolean cancel(final UUID job_id, final boolean may_interrupt) throws RPCException {

        if (submitted_jobs.containsKey(job_id)) {
            final boolean cancelled = submitted_jobs.get(job_id).cancel(may_interrupt);
            if (cancelled) {
                submitted_jobs.remove(job_id);
            }
            return cancelled;
        }
        throw new RemoteWorkerException("Unable to cancel job, worker does not know of any job with the id " + job_id);
    }

    @Override
    public synchronized void shutdown() {

        executor.shutdownNow();
        try {
            unexpose();
        }
        catch (final IOException e) {
            LOGGER.debug("Unable to unexpose the worker server", e);
        }
    }

    /**
     * Gets the address on which this worker is exposed.
     *
     * @return the address on which this worker is exposed
     */

    public InetSocketAddress getAddress() {

        return local_address;
    }

    protected ListeningExecutorService createExecutorService() {

        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new NamingThreadFactory("worker_")));
    }

    private void expose() throws IOException {

        server.expose();
    }

    private void notifyException(final UUID job_id, final Throwable exception) {

        try {
            callback.notifyException(job_id, exception);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            LOGGER.error("failed to notify job exception", e);
        }
    }

    private void unexpose() throws IOException {

        server.unexpose();
    }

    private void notifyCompletion(final UUID job_id, final Serializable result) {

        try {
            callback.notifyCompletion(job_id, result);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            LOGGER.error("failed to notify job completion", e);
        }
    }

    private static synchronized UUID generateJobId() {

        return UUID.randomUUID();
    }

    private class FutureCallbackNotifier implements FutureCallback<Serializable> {

        private final UUID job_id;

        private FutureCallbackNotifier(UUID job_id) {

            this.job_id = job_id;
        }

        @Override
        public void onSuccess(final Serializable result) {

            notifyCompletion(job_id, result);
        }

        @Override
        public void onFailure(final Throwable error) {

            notifyException(job_id, error);
        }
    }

}
