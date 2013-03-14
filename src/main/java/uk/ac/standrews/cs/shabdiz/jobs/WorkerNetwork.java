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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import uk.ac.standrews.cs.jetson.JsonRpcServer;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NamingThreadFactory;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.api.Host;

/**
 * Deploys workers on hosts.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNetwork extends AbstractApplicationNetwork<RemoteWorkerDescriptor> implements WorkerCallback {

    private static final long serialVersionUID = -8888064138251583848L;

    private static final Logger LOGGER = Logger.getLogger(WorkerNetwork.class.getName());
    private static final int EPHEMERAL_PORT = 0;

    private final InetSocketAddress callback_address; // The address on which the callback server is exposed
    private final JsonRpcServer callback_server; // The server which listens to the callbacks  from workers
    private final Map<UUID, PassiveFutureRemoteProxy<? extends Serializable>> id_future_map; // Stores mapping of a job id to the proxy of its pending result
    private final ExecutorService deployment_executor;
    private final WorkerManager worker_manager;

    /**
     * Instantiates a new launcher. Exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public WorkerNetwork() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(new HashSet<File>());
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     * 
     * @param classpath the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public WorkerNetwork(final Set<File> classpath) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(EPHEMERAL_PORT, classpath);
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with the given port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     * 
     * @param callback_server_port the port on which the callback server is exposed
     * @param classpath the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public WorkerNetwork(final int callback_server_port, final Set<File> classpath) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        super("Shabdiz Worker Network");
        id_future_map = new ConcurrentSkipListMap<UUID, PassiveFutureRemoteProxy<? extends Serializable>>();
        deployment_executor = Executors.newCachedThreadPool(new NamingThreadFactory("ShabdizNetwork_"));
        callback_server = new JsonRpcServer(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port), WorkerCallback.class, this, WorkerJsonFactory.getInstance(), deployment_executor);
        expose();
        callback_address = callback_server.getLocalSocketAddress(); // Since the initial server port may be zero, get the actual address of the callback server
        worker_manager = new WorkerManager(this, classpath);
    }

    ExecutorService getExecutor() {

        return deployment_executor;
    }

    InetSocketAddress getCallbackAddress() {

        return callback_address;
    }

    public boolean add(final Host host) {

        return add(new RemoteWorkerDescriptor(host, worker_manager));
    }

    @Override
    public synchronized void notifyCompletion(final UUID job_id, final Serializable result) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setResult(result);
        }
        else {
            LOGGER.info("Launcher was notified about an unknown job completion " + job_id);
        }
    }

    @Override
    public synchronized void notifyException(final UUID job_id, final Exception exception) {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setException(exception);
        }
        else {
            Diagnostic.trace(DiagnosticLevel.NONE, "Launcher was notified about an unknown job exception ", job_id);
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
            deployment_executor.shutdownNow();
            unexpose();
            releaseAllPendingFutures(); // Release the futures which are still pending for notification
        }
        finally {
            super.shutdown();
        }
    }

    <Result extends Serializable> void notifyJobSubmission(final PassiveFutureRemoteProxy<Result> future_remote) {

        id_future_map.put(future_remote.getJobID(), future_remote);
    }

    private void expose() throws IOException {

        callback_server.expose();
    }

    private void unexpose() {

        callback_server.unexpose();
    }

    private void releaseAllPendingFutures() {

        final RPCException unexposed_launcher_exception = new RPCException("Launcher is been shut down, no longer can receive notifications from workers");

        for (final PassiveFutureRemoteProxy<? extends Serializable> future_remote : id_future_map.values()) { // For each future

            if (!future_remote.isDone()) { // Check whether the result is pending
                future_remote.setException(unexposed_launcher_exception); // Tell the pending future that notifications can no longer be received
            }
        }
    }

}
