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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManagerFactory;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.interfaces.IMadfaceManager;
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
 * Deploys workers on a set of added hosts and provides a coordinated proxy to communicate with them.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class Launcher implements ILauncher, ILauncherCallback {

    private static final int EPHEMERAL_PORT = 0;

    private final InetSocketAddress callback_server_address; // The address on which the callback server is exposed
    private final LauncherCallbackRemoteServer callback_server; // The server which listens to the callbacks  from workers
    private final Map<UUID, FutureRemoteProxy<? extends Serializable>> id_future_map; // Stores mapping of a job id to its remote result

    private final IMadfaceManager madface_manager;

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new coordinator. The coordinator is exposed on local address on an ephemeral port number.
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
     * Instantiates a new  coordinator and exposes the coordinator on local address on an <i>ephemeral</i> port number.
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
     * Instantiates a new coordinator node and starts a local server which listens to the notifications from workers on the given port number.
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

        application_lib_urls.addAll(LibraryUtil.getShabdizApplicationLibraryURLs()); // Add the libraries needed by Shabdiz itself

        id_future_map = new ConcurrentSkipListMap<UUID, FutureRemoteProxy<? extends Serializable>>();

        callback_server = new LauncherCallbackRemoteServer(this);
        expose(NetworkUtil.getLocalIPv4InetSocketAddress(callback_server_port));

        callback_server_address = callback_server.getAddress(); // Since the initial server port may be zero, get the actual address of the callback server

        madface_manager = MadfaceManagerFactory.makeMadfaceManager();
        madface_manager.configureApplication(new WorkerManager());
        madface_manager.configureApplication(application_lib_urls);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized IWorker deployWorkerOnHost(final HostDescriptor host_descriptor) throws Exception {

        configureApplicationDeploymentParams(host_descriptor); // Configure the host's application deployment parameters

        madface_manager.setHostScanning(true); // Start MADFACE scanners
        madface_manager.deploy(host_descriptor); // Deploy worker on host
        madface_manager.waitForHostToReachState(host_descriptor, HostState.RUNNING); // Block until the worker is running
        madface_manager.setHostScanning(false); // Stop MADFACE scanners

        host_descriptor.shutdown(); // XXX discuss whether to shut down the process manager of host descriptor

        final IWorker worker = new Worker(this, host_descriptor.getInetSocketAddress()); // XXX dsicuss (Worker) host_descriptor.getApplicationReference(); // Retrieval of  the remote proxy of the deployed worker
        return worker; // return the coordinated proxy to the worker
    }

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setResult(result);
        }
        else {
            Diagnostic.trace(DiagnosticLevel.RUN, "Launcher was notified about an unknown job completion ", job_id);
        }
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        if (id_future_map.containsKey(job_id)) {
            id_future_map.get(job_id).setException(exception);
        }
        else {
            Diagnostic.trace(DiagnosticLevel.RUN, "Launcher was notified about an unknown job exception ", job_id);
        }
    }

    /**
     * Unexposes the coordinator Server which breaks the communication to the workers deployed by this coordinator.
     * @see ILauncher#shutdown()
     */
    @Override
    public void shutdown() {

        unexpose();
        madface_manager.shutdown();
        // XXX discuss whether to clear out all the notifications
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    <Result extends Serializable> Future<Result> submitJob(final IJobRemote<Result> job, final InetSocketAddress worker_address) throws RPCException {

        final UUID job_id = WorkerRemoteProxyFactory.getProxy(worker_address).submitJob(job);
        final FutureRemoteProxy<Result> future_remote = new FutureRemoteProxy<Result>(job_id, worker_address);
        id_future_map.put(job_id, future_remote);

        return future_remote;
    }

    void shutdownWorker(final InetSocketAddress worker_address) throws RPCException {

        WorkerRemoteProxyFactory.getProxy(worker_address).shutdown();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void configureApplicationDeploymentParams(final HostDescriptor host_descriptor) {

        final Object[] application_deployment_params = new Object[]{callback_server_address};
        host_descriptor.applicationDeploymentParams(application_deployment_params);
    }

    private void expose(final InetSocketAddress expose_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        callback_server.setLocalAddress(expose_address.getAddress());
        callback_server.setPort(expose_address.getPort());

        callback_server.start(true);
    }

    private void unexpose() {

        try {
            callback_server.stop();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "Unable to stop coordinator server, because: ", e.getMessage(), e);
        }
    }
}
