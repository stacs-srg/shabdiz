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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import org.mashti.jetson.Server;
import org.mashti.jetson.ServerFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.exception.TransportException;
import org.mashti.jetson.lean.LeanServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

/**
 * Presents a network of {@link Worker workers}. {@link Host Hosts} are added using {@link #add(Host)}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNetwork extends ApplicationNetwork implements WorkerCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerNetwork.class);
    private static final int EPHEMERAL_PORT = 0;
    private final InetSocketAddress callback_address; // The address on which the callback server is exposed
    private final ConcurrentSkipListMap<UUID, FutureRemote<? extends Serializable>> id_future_map; // Stores mapping of a job id to the proxy of its pending result
    private final Server callback_server; // The server which listens to the callbacks  from workers
    private final WorkerManager worker_manager;
    private final ServerFactory<WorkerCallback> callback_server_factory;

    /**
     * Instantiates a new launcher. Exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public WorkerNetwork() throws IOException {

        this(EPHEMERAL_PORT);
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with the given port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     *
     * @param callback_server_port the port on which the callback server is exposed
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public WorkerNetwork(final int callback_server_port) throws IOException {

        super("Shabdiz Worker Network");
        id_future_map = new ConcurrentSkipListMap<UUID, FutureRemote<? extends Serializable>>();
        callback_server_factory = new LeanServerFactory<WorkerCallback>(WorkerCallback.class);
        callback_server = callback_server_factory.createServer(this);
        callback_server.setBindAddress(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port));
        expose();
        callback_address = callback_server.getLocalSocketAddress(); // Since the initial server port may be zero, get the actual address of the callback server
        worker_manager = new WorkerManager(this);
    }

    /**
     * Adds a host to this worker network.
     *
     * @param host the host to add
     * @return the ApplicationDescriptor associated with the added host, or {@code null} if the host was not added
     */
    public ApplicationDescriptor add(final Host host) {

        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, worker_manager);
        return add(descriptor) ? descriptor : null;
    }

    @Override
    public synchronized void notifyCompletion(final UUID job_id, final Serializable result) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).set(result);
        }
        else {
            LOGGER.info("Launcher was notified about an unknown job completion " + job_id);
        }
    }

    @Override
    public synchronized void notifyException(final UUID job_id, final Throwable exception) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setException(exception);
        }
        else {
            LOGGER.info("Launcher was notified about an unknown job exception " + job_id);
        }
    }

    public void addMavenDependency(final String group_id, final String artifact_id, final String version, final String classifier) {

        worker_manager.addMavenDependency(group_id, artifact_id, version, classifier);
    }

    /**
     * Unexposes the launcher callback server which listens to the worker notifications. Shuts down worker deployment mechanisms.
     * Note that any pending {@link Future} will end in exception.
     *
     * @see WorkerNetwork#shutdown()
     */
    @Override
    public void shutdown() {

        try {
            try {
                callback_server.unexpose();
            }
            catch (final IOException e) {
                LOGGER.debug("failed to unexpose callback server", e);
            }
            callback_server_factory.shutdown();
            worker_manager.shutdown();
            releaseAllPendingFutures(); // Release the futures which are still pending for notification
        }
        finally {
            super.shutdown();
        }
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(super.hashCode(), callback_address.hashCode(), id_future_map.hashCode(), callback_server.hashCode(), worker_manager.hashCode(), callback_server_factory.hashCode());
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof WorkerNetwork)) { return false; }
        if (!super.equals(other)) { return false; }

        final WorkerNetwork that = (WorkerNetwork) other;

        if (!callback_address.equals(that.callback_address)) { return false; }
        if (!callback_server.equals(that.callback_server)) { return false; }
        if (!callback_server_factory.equals(that.callback_server_factory)) { return false; }
        if (!id_future_map.equals(that.id_future_map)) { return false; }

        return worker_manager.equals(that.worker_manager);
    }

    private void expose() throws IOException {

        callback_server.expose();
    }

    InetSocketAddress getCallbackAddress() {

        return callback_address;
    }

    private void releaseAllPendingFutures() {

        final RPCException unexposed_launcher_exception = new TransportException("Launcher is been shut down, no longer can receive notifications from workers");

        for (final FutureRemote<? extends Serializable> future_remote : id_future_map.values()) {
            if (!future_remote.isDone()) {
                future_remote.setException(unexposed_launcher_exception); // Tell the pending future that notifications can no longer be received
            }
        }
    }

    <Result extends Serializable> void notifyJobSubmission(final FutureRemote<Result> future_remote) {

        id_future_map.put(future_remote.getJobID(), future_remote);
    }

}
