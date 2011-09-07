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
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerNode;

/**
 * An implementation of {@link IWorkerNode}. It notifies the coordinator about the completion of the submitted jobs.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class Worker implements IWorkerNode {

    private static final int THREAD_POOL_SIZE = 10; // TODO add a parameter for it in entry point server

    private final InetSocketAddress local_address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> id_future_map;
    private final LauncherCallbackRemoteProxy launcher_callback_proxy;
    private final WorkerRemoteServer server;

    /**
     * Instantiates a new worker.
     *
     * @param local_address the address on which the worker is exposed
     * @param coordinator_address the coordinator address
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    Worker(final InetSocketAddress local_address, final InetSocketAddress coordinator_address) throws IOException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException, RPCException {

        this.local_address = local_address;

        launcher_callback_proxy = makeCoordinatorProxy(coordinator_address);
        id_future_map = new ConcurrentSkipListMap<UUID, Future<? extends Serializable>>();
        exexcutor_service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        server = new WorkerRemoteServer(this);
        expose();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public UUID submitJob(final IJobRemote<? extends Serializable> job) {

        final UUID job_id = generateJobId();

        exexcutor_service.execute(new Runnable() {

            @Override
            public void run() {

                final Future<? extends Serializable> real_future = exexcutor_service.submit(job);
                id_future_map.put(job_id, real_future);

                try {

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
    public synchronized void shutdown() {

        exexcutor_service.shutdownNow();

        if (exexcutor_service.isTerminated()) {

            try {
                unexpose();
            }
            catch (final IOException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Unable to unexpose the worker server, because: ", e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    boolean submittedJobsContain(final UUID job_id) {

        return id_future_map.containsKey(job_id);
    }

    Future<? extends Serializable> getFutureById(final UUID job_id) {

        return id_future_map.get(job_id);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    void handleCompletion(final UUID job_id, final Serializable result) {

        try {

            launcher_callback_proxy.notifyCompletion(job_id, result); // Tell launcher about the result
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exception
            e.printStackTrace();
        }
    }

    void handleException(final UUID job_id, final Exception exception) {

        try {

            launcher_callback_proxy.notifyException(job_id, exception); // Tell launcher about the exception
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the coordinator rpc exception
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private LauncherCallbackRemoteProxy makeCoordinatorProxy(final InetSocketAddress coordinator_address) {

        return LauncherCallbackRemoteProxyFactory.getProxy(coordinator_address);
    }

    private void expose() throws IOException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException, RPCException {

        server.setLocalAddress(local_address.getAddress());
        server.setPort(local_address.getPort());

        server.start(true);
    }

    private void unexpose() throws IOException {

        server.stop();
    }

    private static synchronized UUID generateJobId() {

        return UUID.randomUUID();
    }
}
