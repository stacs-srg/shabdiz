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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncher;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncherCallback;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.util.LibraryUtil;

/**
 * Deploys workers on hosts. Uses MADFACE to deploy workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Launcher implements ILauncher, ILauncherCallback {

    private static final int EPHEMERAL_PORT = 0;
    private static final List<String> DEFAULT_WORKER_JVM_PARAMS = Arrays.asList(new String[]{"-Xmx128m"}); // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private final InetSocketAddress callback_server_address; // The address on which the callback server is exposed
    private final LauncherCallbackRemoteServer callback_server; // The server which listens to the callbacks  from workers
    private final Map<UUID, FutureRemoteProxy<? extends Serializable>> id_future_map; // Stores mapping of a job id to the proxy of its pending result

    private final WorkerRemoteFactory worker_remote_factory;

    private final Set<URL> application_lib_urls;

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
    public Launcher() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(new HashSet<URL>());
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with an <i>ephemeral</i> port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     * 
     * @param application_lib_urls the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Launcher(final Set<URL> application_lib_urls) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this(EPHEMERAL_PORT, application_lib_urls);
    }

    /**
     * Instantiates a new launcher and exposes the launcher callback on local address with the given port number.
     * The given application library URLs are loaded on any worker which is deployed by this launcher.
     * 
     * @param callback_server_port the port on which the callback server is exposed
     * @param application_lib_urls the application library URLs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public Launcher(final int callback_server_port, final Set<URL> application_lib_urls) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        this.application_lib_urls = application_lib_urls;
        this.application_lib_urls.addAll(LibraryUtil.getShabdizApplicationLibraryURLs()); // Add the libraries needed by Shabdiz itself

        id_future_map = new ConcurrentSkipListMap<UUID, FutureRemoteProxy<? extends Serializable>>();

        callback_server = new LauncherCallbackRemoteServer(this);
        expose(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port));
        callback_server_address = callback_server.getAddress(); // Since the initial server port may be zero, get the actual address of the callback server

        worker_remote_factory = new WorkerRemoteFactory();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public IWorker deployWorkerOnHost(final HostDescriptor host_descriptor) throws Exception {

        configureHostDescriptor(host_descriptor); // Prepare host for deployment
        worker_remote_factory.createNode(host_descriptor); // Deploy.

        return new Worker(this, host_descriptor.getInetSocketAddress()); // Return the smart proxy to the worker remote.
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
     * @see ILauncher#shutdown()
     */
    @Override
    public void shutdown() {

        unexpose();
        releaseAllPendingFutures(); // Release the futures which are still pending for notification
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    synchronized <Result extends Serializable> Future<Result> submitJob(final IJobRemote<Result> job, final InetSocketAddress worker_address) throws RPCException {

        final UUID job_id = WorkerRemoteProxyFactory.getProxy(worker_address).submitJob(job);
        final FutureRemoteProxy<Result> future_remote = new FutureRemoteProxy<Result>(job_id, worker_address);
        id_future_map.put(job_id, future_remote);

        return future_remote;
    }

    void shutdownWorker(final InetSocketAddress worker_address) throws RPCException {

        WorkerRemoteProxyFactory.getProxy(worker_address).shutdown();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void configureHostDescriptor(final HostDescriptor host_descriptor) {

        final Object[] application_deployment_params = new Object[]{callback_server_address};
        host_descriptor.applicationDeploymentParams(application_deployment_params);
        if (host_descriptor.getJVMDeploymentParams() == null) {
            host_descriptor.jvmDeploymentParams(DEFAULT_WORKER_JVM_PARAMS);
        }
        host_descriptor.applicationURLs(application_lib_urls);
    }

    private void expose(final InetSocketAddress expose_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        callback_server.setLocalAddress(expose_address.getAddress());
        callback_server.setPort(expose_address.getPort());

        //        callback_server.start(true);
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
