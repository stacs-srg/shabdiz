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
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.interfaces.Launcher;
import uk.ac.standrews.cs.shabdiz.interfaces.LauncherCallback;
import uk.ac.standrews.cs.shabdiz.interfaces.Worker;

/**
 * Deploys workers on hosts. Uses MADFACE to deploy workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DefaultLauncher implements Launcher, LauncherCallback {

    private static final int EPHEMERAL_PORT = 0;
    private static final String WORKER_JVM_ARGUMENTS = "-Xmx128m"; // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private final InetSocketAddress callback_address; // The address on which the callback server is exposed
    private final LauncherCallbackRemoteServer callback_server; // The server which listens to the callbacks  from workers
    private final Map<UUID, FutureRemoteProxy<? extends Serializable>> id_future_map; // Stores mapping of a job id to the proxy of its pending result

    private final RemoteJavaProcessBuilder worker_process_builder;

    // -------------------------------------------------------------------------------------------------------------------------------

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
    public DefaultLauncher() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

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
    public DefaultLauncher(final Set<File> classpath) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

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
    public DefaultLauncher(final int callback_server_port, final Set<File> classpath) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        id_future_map = new ConcurrentSkipListMap<UUID, FutureRemoteProxy<? extends Serializable>>();
        callback_server = new LauncherCallbackRemoteServer(this);
        expose(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port));
        callback_address = callback_server.getAddress(); // Since the initial server port may be zero, get the actual address of the callback server
        worker_process_builder = createRemoteJavaProcessBuiler(classpath, WORKER_JVM_ARGUMENTS);

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public Worker deployWorkerOnHost(final Host host) throws IOException, InterruptedException {

        final Process worker_process = worker_process_builder.start(host);
        final InetSocketAddress worker_address = getWorkerRemoteAddressFromProcessOutput(worker_process);

        return new DefaultWorker(this, worker_address, worker_process); // Return the smart proxy to the worker remote.
    }

    private InetSocketAddress getWorkerRemoteAddressFromProcessOutput(final Process worker_process) throws UnknownHostException, IOException {

        // TODO add timeout
        InetSocketAddress worker_address;
        final InputStreamReader reader = new InputStreamReader(worker_process.getInputStream());
        final BufferedReader br = new BufferedReader(reader); // this is not closed on purpose.  the stream belongs to Process instance.
        do {
            final String output_line = br.readLine();
            worker_address = WorkerNodeServer.parseOutputLine(output_line);
        }
        while (worker_address == null);
        return worker_address;
    }

    @Override
    public synchronized void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setResult(result);
        }
        else {
            Diagnostic.trace(DiagnosticLevel.NONE, "Launcher was notified about an unknown job completion ", job_id);
        }
    }

    @Override
    public synchronized void notifyException(final UUID job_id, final Exception exception) throws RPCException {

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
     * @see DefaultLauncher#shutdown()
     */
    @Override
    public void shutdown() {

        unexpose();
        releaseAllPendingFutures(); // Release the futures which are still pending for notification
    }

    <Result extends Serializable> void notifyJobSubmission(final FutureRemoteProxy<Result> future_remote) {

        id_future_map.put(future_remote.getJobID(), future_remote);
    }

    private RemoteJavaProcessBuilder createRemoteJavaProcessBuiler(final Set<File> classpath, final String jvm_arguments) {

        final RemoteJavaProcessBuilder process_builder = new RemoteJavaProcessBuilder(WorkerNodeServer.class);
        process_builder.addCommandLineArgument(WorkerNodeServer.LAUNCHER_CALLBACK_ADDRESS_KEY + NetworkUtil.formatHostAddress(callback_address));
        process_builder.addJVMArgument(jvm_arguments);
        process_builder.addClasspath(classpath);
        process_builder.addCurrentJVMClasspath();
        return process_builder;
    }

    private void expose(final InetSocketAddress expose_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        callback_server.setLocalAddress(expose_address.getAddress());
        callback_server.setPort(expose_address.getPort());
        callback_server.startWithNoRegistry();
    }

    private void unexpose() {

        try {
            callback_server.stop();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "Unable to stop launcher callback server, because: ", e.getMessage(), e);
        }
    }

    private void releaseAllPendingFutures() {

        final RPCException unexposed_launcher_exception = new RPCException("Launcher is been shut down, no longer can receive notifications from workers");

        for (final FutureRemoteProxy<? extends Serializable> future_remote : id_future_map.values()) { // For each future

            if (!future_remote.isDone()) { // Check whether the result is pending

                future_remote.setException(unexposed_launcher_exception); // Tell the pending future that notifications can no longer be received
            }
        }
    }
}
