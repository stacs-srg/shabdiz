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
import com.staticiser.jetson.exception.JsonRpcException;
import com.staticiser.jetson.exception.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Deploys workers on hosts.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNetwork extends ApplicationNetwork implements WorkerCallback {

    private static final long serialVersionUID = -8888064138251583848L;
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerNetwork.class);
    private static final int EPHEMERAL_PORT = 0;
    private final InetSocketAddress callback_address; // The address on which the callback server is exposed
    private final transient ConcurrentSkipListMap<UUID, PassiveFutureRemoteProxy<? extends Serializable>> id_future_map; // Stores mapping of a job id to the proxy of its pending result
    private final transient Server callback_server; // The server which listens to the callbacks  from workers
    private final transient WorkerManager worker_manager;
    private final transient ServerFactory<WorkerCallback> callback_server_factory;

    /**
     * Instantiates a new launcher. Exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     *
     * @throws UnknownHostException
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws AlreadyBoundException the already bound exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public WorkerNetwork() throws UnknownHostException, IOException {

        this(new HashSet<File>());
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     *
     * @param classpath the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public WorkerNetwork(final Set<File> classpath) throws UnknownHostException, IOException {

        this(EPHEMERAL_PORT, classpath);
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with the given port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     *
     * @param callback_server_port the port on which the callback server is exposed
     * @param classpath the application library URLs
     * @throws UnknownHostException
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public WorkerNetwork(final int callback_server_port, final Set<File> classpath) throws UnknownHostException, IOException {

        super("Shabdiz Worker Network");
        id_future_map = new ConcurrentSkipListMap<UUID, PassiveFutureRemoteProxy<? extends Serializable>>();
        callback_server_factory = new ServerFactory<WorkerCallback>(WorkerCallback.class, WorkerJsonFactory.getInstance());
        callback_server = callback_server_factory.createServer(this);
        callback_server.setBindAddress(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port));
        expose();
        callback_address = callback_server.getLocalSocketAddress(); // Since the initial server port may be zero, get the actual address of the callback server
        System.out.println(callback_address);
        worker_manager = new WorkerManager(this, classpath);
    }

    private void expose() throws IOException {

        callback_server.expose();
    }

    InetSocketAddress getCallbackAddress() {

        return callback_address;
    }

    public ApplicationDescriptor add(final Host host) {

        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, worker_manager);
        return add(descriptor) ? descriptor : null;
    }

    @Override
    public synchronized void notifyCompletion(final UUID job_id, final Serializable result) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setResult(result);
        } else {
            LOGGER.info("Launcher was notified about an unknown job completion " + job_id);
        }
    }

    @Override
    public synchronized void notifyException(final UUID job_id, final Exception exception) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setException(exception);
        } else {
            LOGGER.info("Launcher was notified about an unknown job exception " + job_id);
        }
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
            } catch (final IOException e) {
                LOGGER.debug("failed to unexpose callback server", e);
            }
            callback_server_factory.shutdown();
            worker_manager.shutdown();
            releaseAllPendingFutures(); // Release the futures which are still pending for notification
        } finally {
            super.shutdown();
        }
    }

    private void releaseAllPendingFutures() {

        final JsonRpcException unexposed_launcher_exception = new TransportException("Launcher is been shut down, no longer can receive notifications from workers");

        for (final PassiveFutureRemoteProxy<? extends Serializable> future_remote : id_future_map.values()) { // For each future

            if (!future_remote.isDone()) { // Check whether the result is pending
                future_remote.setException(unexposed_launcher_exception); // Tell the pending future that notifications can no longer be received
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerNetwork)) return false;
        if (!super.equals(o)) return false;

        final WorkerNetwork that = (WorkerNetwork) o;

        if (callback_address != null ? !callback_address.equals(that.callback_address) : that.callback_address != null)
            return false;
        if (callback_server != null ? !callback_server.equals(that.callback_server) : that.callback_server != null)
            return false;
        if (callback_server_factory != null ? !callback_server_factory.equals(that.callback_server_factory) : that.callback_server_factory != null)
            return false;
        if (id_future_map != null ? !id_future_map.equals(that.id_future_map) : that.id_future_map != null)
            return false;
        if (worker_manager != null ? !worker_manager.equals(that.worker_manager) : that.worker_manager != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (callback_address != null ? callback_address.hashCode() : 0);
        result = 31 * result + (callback_server != null ? callback_server.hashCode() : 0);
        result = 31 * result + (id_future_map != null ? id_future_map.hashCode() : 0);
        result = 31 * result + (worker_manager != null ? worker_manager.hashCode() : 0);
        result = 31 * result + (callback_server_factory != null ? callback_server_factory.hashCode() : 0);
        return result;
    }

    <Result extends Serializable> void notifyJobSubmission(final PassiveFutureRemoteProxy<Result> future_remote) {

        id_future_map.put(future_remote.getJobID(), future_remote);
    }

}
