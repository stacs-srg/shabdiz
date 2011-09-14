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
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;

/**
 * An implementation of {@link IWorkerRemote} which notifies the launcher about the completion of the submitted jobs on a given callback address.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class WorkerRemote implements IWorkerRemote {

    private static final int THREAD_POOL_SIZE = 10; // TODO add a parameter for it in entry point server

    private final InetSocketAddress local_address;
    private final ExecutorService exexcutor_service;
    private final ConcurrentSkipListMap<UUID, Future<? extends Serializable>> id_future_map;
    private final WorkerRemoteServer server;

    private final InetSocketAddress launcher_callback_address;

    /**
     * Instantiates a new worker.
     *
     * @param local_address the address on which the worker is exposed
     * @param launcher_callback_address the launcher callback address
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    WorkerRemote(final InetSocketAddress local_address, final InetSocketAddress launcher_callback_address) throws IOException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException, RPCException {

        this.local_address = local_address;
        this.launcher_callback_address = launcher_callback_address;

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

        //        try {
        //
        //            final RegistryServer local_registry_server = RegistryFactory.FACTORY.getLocalRegistryServer();
        //
        //            if (local_registry_server != null) {
        //                local_registry_server.stop();
        //            }
        //            else {
        //                RegistryFactory.FACTORY.getRegistry().shutdown();
        //            }
        //        }
        //        catch (final Exception e) {
        //
        //            Diagnostic.trace(DiagnosticLevel.NONE, "Unable to stop the local registry server, because: ", e.getMessage(), e);
        //        }

        try {
            unexpose();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.NONE, "Unable to unexpose the worker server, because: ", e.getMessage(), e);
        }

        //        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        //        ThreadGroup parentGroup;
        //        while ((parentGroup = rootGroup.getParent()) != null) {
        //            rootGroup = parentGroup;
        //        }
        //
        //        Thread[] threads = new Thread[rootGroup.activeCount()];
        //        while (rootGroup.enumerate(threads, true) == threads.length) {
        //            threads = new Thread[threads.length * 2];
        //        }
        //
        //        for (final Thread t : threads) {
        //            System.out.println("thread: " + t.getName());
        //        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    boolean submittedJobsContain(final UUID job_id) {

        return id_future_map.containsKey(job_id);
    }

    Future<? extends Serializable> getFutureById(final UUID job_id) {

        return id_future_map.get(job_id);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void handleCompletion(final UUID job_id, final Serializable result) {

        try {

            LauncherCallbackRemoteProxyFactory.getProxy(launcher_callback_address).notifyCompletion(job_id, result); // Tell launcher about the result
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the launcher callback rpc exception
            e.printStackTrace();
        }
    }

    private void handleException(final UUID job_id, final Exception exception) {

        try {

            LauncherCallbackRemoteProxyFactory.getProxy(launcher_callback_address).notifyException(job_id, exception); // Tell launcher about the exception
        }
        catch (final RPCException e) {
            // XXX discuss whether to use some sort of error manager  which handles the launcher callback rpc exception
            e.printStackTrace();
        }
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
