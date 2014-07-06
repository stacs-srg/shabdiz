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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
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

    private static final ServerFactory<WorkerRemote> SERVER_FACTORY = new LeanServerFactory<>(WorkerRemote.class);
    private static final ClientFactory<WorkerCallback> CLIENT_FACTORY = new LeanClientFactory<>(WorkerCallback.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkerRemote.class);
    private final ExecutorService executor;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> submitted_jobs;
    private final Server server;
    private final WorkerCallback callback;
    private final ExecutorService callback_executor = Executors.newCachedThreadPool(new NamedThreadFactory("worker_callback_", true));
    private static final NotificationErrorHandler NOTIFICATION_ERROR_HANDLER = new NotificationErrorHandler();

    protected DefaultWorkerRemote(final InetSocketAddress local_address, final InetSocketAddress callback_address) throws IOException {

        callback = CLIENT_FACTORY.get(callback_address);
        submitted_jobs = new ConcurrentSkipListMap<>();
        executor = createExecutorService();
        server = SERVER_FACTORY.createServer(this);
        init(local_address);
    }

    @Override
    public CompletableFuture<Void> submit(final UUID job_id, final Job<? extends Serializable> job) {

        final CompletableFuture<Serializable> future_result = new CompletableFuture<>();
        submitted_jobs.put(job_id, future_result);

        return CompletableFuture.runAsync(() -> {

            try {

                final Serializable result = job.call();
                future_result.complete(result);

                LOGGER.debug("job {} completed normally", job_id);
                callback.notifyCompletion(job_id, result).exceptionally(NOTIFICATION_ERROR_HANDLER);
            }
            catch (final Throwable error) {

                future_result.completeExceptionally(error);

                LOGGER.debug("job {} completed exceptionally: {}", job_id, error);
                callback.notifyException(job_id, error).exceptionally(NOTIFICATION_ERROR_HANDLER);
            }
            finally {
                submitted_jobs.remove(job_id);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> cancel(final UUID id, final boolean may_interrupt) {

        final CompletableFuture<Boolean> future_cancellation = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            if (submitted_jobs.containsKey(id)) {
                final boolean cancelled = submitted_jobs.get(id).cancel(may_interrupt);
                if (cancelled) {
                    submitted_jobs.remove(id);
                }

                LOGGER.debug("cancelling job with ID {}, cancelled? {}", id, cancelled);
                future_cancellation.complete(cancelled);
            }
            else {
                LOGGER.debug("received cancellation request for a an unknown job {} ", id);
                future_cancellation.completeExceptionally(new UnknownJobException("Unable to cancel job, worker does not know of any job with the id " + id));
            }
        }, callback_executor);

        return future_cancellation;
    }

    @Override
    public CompletableFuture<Void> shutdown() {

        return CompletableFuture.runAsync(() -> {
            executor.shutdownNow();
            try {
                server.unexpose();
            }
            catch (final IOException e) {
                LOGGER.debug("Unable to unexpose the worker server", e);
            }
        }, callback_executor);
    }

    /**
     * Gets the address on which this worker is exposed.
     *
     * @return the address on which this worker is exposed
     */
    public InetSocketAddress getAddress() {

        return server.getLocalSocketAddress();
    }

    protected ExecutorService createExecutorService() {

        return Executors.newCachedThreadPool(new NamedThreadFactory("worker_", true));
    }

    private void init(final InetSocketAddress local_address) throws IOException {

        server.setBindAddress(local_address);
        server.expose();
    }

    private static class NotificationErrorHandler implements Function<Throwable, Void> {

        @Override
        public Void apply(final Throwable notification_error) {

            LOGGER.error("failed to notify the callback server", notification_error);
            return null;
        }
    }
}
