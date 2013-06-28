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

import com.staticiser.jetson.Server;
import com.staticiser.jetson.ServerFactory;
import com.staticiser.jetson.exception.RPCException;
import com.staticiser.jetson.json.JsonServerFactory;
import com.staticiser.jetson.util.NamingThreadFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DefaultWorkerRemote} which notifies the launcher about the completion of the submitted jobs on a given callback address.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultWorkerRemote implements WorkerRemote {

    private static final ServerFactory<WorkerRemote> SERVER_FACTORY = new JsonServerFactory<WorkerRemote>(WorkerRemote.class, WorkerJsonFactory.getInstance());
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkerRemote.class);
    private final InetSocketAddress local_address;
    private final ExecutorService executor;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> submitted_jobs;
    private final Server server;
    private final WorkerCallback callback;

    protected DefaultWorkerRemote(final InetSocketAddress local_address, final InetSocketAddress callback_address) throws IOException {

        callback = CallbackProxyFactory.getProxy(callback_address);
        submitted_jobs = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        executor = createExecutorService();
        server = SERVER_FACTORY.createServer(this);
        server.setBindAddress(local_address);
        expose();
        this.local_address = server.getLocalSocketAddress();
    }

    protected ExecutorService createExecutorService() {

        return Executors.newCachedThreadPool(new NamingThreadFactory("worker_"));
    }

    private void expose() throws IOException {

        server.expose();
    }

    @Override
    public UUID submitJob(final Job<? extends Serializable> job) {

        final UUID job_id = generateJobId();
        executor.execute(new Runnable() {

            @Override
            public void run() {

                final Future<? extends Serializable> real_future = executor.submit(job);
                submitted_jobs.put(job_id, real_future);

                try {
                    //TODO this blocks until the job is complete; Could be written nicer so that it runs after completion.
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

    private void handleException(final UUID job_id, final Exception exception) {

        try {
            callback.notifyException(job_id, exception);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            //TODO use some sort of error manager  which handles the launcher callback rpc exception
            LOGGER.error("failed to notify job exception", e);
        }
    }

    private void unexpose() throws IOException {

        server.unexpose();
    }

    private void handleCompletion(final UUID job_id, final Serializable result) {

        try {
            callback.notifyCompletion(job_id, result);
            submitted_jobs.remove(job_id);
        }
        catch (final RPCException e) {
            //TODO discuss whether to use some sort of error manager  which handles the launcher callback rpc exception
            LOGGER.error("failed to notify job completion", e);
        }
    }

    private static synchronized UUID generateJobId() {

        return UUID.randomUUID();
    }

    /**
     * Gets the address on which this worker is exposed.
     *
     * @return the address on which this worker is exposed
     */
    public InetSocketAddress getAddress() {

        return local_address;
    }
}
