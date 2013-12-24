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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.lean.LeanClientFactory;
import org.mashti.jetson.lean.LeanServerFactory;
import org.mashti.jetson.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents a {@link WorkerRemote worker} that performs a given job and notifies the launcher about the completion of the submitted jobs on a given callback address.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultWorkerRemote implements WorkerRemote {

    private static final ServerFactory<WorkerRemote> SERVER_FACTORY = new LeanServerFactory<WorkerRemote>(WorkerRemote.class);
    private static final ClientFactory<WorkerCallback> CLIENT_FACTORY = new LeanClientFactory<WorkerCallback>(WorkerCallback.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkerRemote.class);
    private final ListeningExecutorService executor;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> submitted_jobs;
    private final Server server;
    private final WorkerCallback callback;
    private final ExecutorService callback_executor = Executors.newCachedThreadPool(new NamedThreadFactory("worker_callback_", true));

    protected DefaultWorkerRemote(final InetSocketAddress local_address, final InetSocketAddress callback_address) throws IOException {

        callback = CLIENT_FACTORY.get(callback_address);
        submitted_jobs = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        executor = createExecutorService();
        server = SERVER_FACTORY.createServer(this);
        init(local_address);
    }

    @Override
    public synchronized UUID submit(final Job<? extends Serializable> job) {

        final UUID id = UUID.randomUUID();
        final ListenableFuture<? extends Serializable> future = executor.submit(job);
        final FutureCallbackNotifier future_callback = new FutureCallbackNotifier(id);

        Futures.addCallback(future, future_callback, callback_executor);
        LOGGER.debug("submitted job {} with ID {}", job, id);
        return id;
    }

    @Override
    public synchronized boolean cancel(final UUID id, final boolean may_interrupt) throws RPCException {

        if (submitted_jobs.containsKey(id)) {
            final boolean cancelled = submitted_jobs.get(id).cancel(may_interrupt);
            if (cancelled) {
                submitted_jobs.remove(id);
            }
            LOGGER.debug("cancelling job with ID {}, cancelled? {}", id, cancelled);
            return cancelled;
        }
        LOGGER.debug("received cancellation request for a an unknown job with id ", id);
        throw new UnknownJobException("Unable to cancel job, worker does not know of any job with the id " + id);
    }

    @Override
    public synchronized void shutdown() {

        executor.shutdownNow();
        try {
            server.unexpose();
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

        return server.getLocalSocketAddress();
    }

    protected ListeningExecutorService createExecutorService() {

        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new NamedThreadFactory("worker_", true)));
    }

    private void init(final InetSocketAddress local_address) throws IOException {

        server.setBindAddress(local_address);
        server.expose();
    }

    private class FutureCallbackNotifier implements FutureCallback<Serializable> {

        private final UUID job_id;

        private FutureCallbackNotifier(final UUID job_id) {

            this.job_id = job_id;
        }

        @Override
        public void onSuccess(final Serializable result) {

            try {
                callback.notifyCompletion(job_id, result);
                submitted_jobs.remove(job_id);
            }
            catch (final RPCException e) {
                LOGGER.error("failed to notify job completion", e);
            }
        }

        @Override
        public void onFailure(final Throwable error) {

            try {
                callback.notifyException(job_id, error);
                submitted_jobs.remove(job_id);
            }
            catch (final RPCException e) {
                LOGGER.error("failed to notify job exception", e);
            }
        }
    }

}
