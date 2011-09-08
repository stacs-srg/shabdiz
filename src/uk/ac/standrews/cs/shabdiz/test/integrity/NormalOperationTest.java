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
package uk.ac.standrews.cs.shabdiz.test.integrity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.impl.Launcher;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

/**
 * Tests whether a result/exception return by a job is working.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class NormalOperationTest {

    private static final int[] WORKER_NETWORK_SIZE = {5};
    static final String HELLO = "hello";

    /**
     * Sets the up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        // Kill any lingering Shabdiz Worker processes.
        final HostDescriptor local_host_descriptor = new HostDescriptor();
        local_host_descriptor.getProcessManager().killMatchingProcesses(WorkerNodeServer.class.getSimpleName());
        local_host_descriptor.shutdown();
    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void sayHelloTest() throws Exception {

        for (final int size : WORKER_NETWORK_SIZE) {

            System.out.println(">>> Worker network size : " + size);

            final Launcher coordinator = new Launcher();
            System.out.println(" deploying workers");
            final Set<IWorker> workers = deployWorkers(coordinator, size);
            System.out.println("done deploying workers");
            for (final IWorker worker : workers) {
                final Future<String> future = worker.submit(new SayHelloRemoteJob());

                Assert.assertEquals(future.get(), HELLO);

                try {
                    worker.shutdown();
                }
                catch (final RPCException e) {
                    // ignore
                }
            }

            coordinator.shutdown();
            System.out.println(">>> Done");
        }
    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void throwExeptionTest() throws Exception {

        for (final int size : WORKER_NETWORK_SIZE) {

            System.out.println(">>> Worker network size : " + size);

            final Launcher coordinator = new Launcher();
            System.out.println(" deploying workers");
            final Set<IWorker> workers = deployWorkers(coordinator, size);
            System.out.println("done deploying workers");
            for (final IWorker worker : workers) {
                final Future<String> future = worker.submit(new NullPointerExceptionRemoteJob());
                try {
                    future.get();
                }
                catch (final ExecutionException e) {
                    Assert.assertTrue(e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("test"));
                }

                try {
                    worker.shutdown();
                }
                catch (final RPCException e) {
                    // ignore
                }
            }

            coordinator.shutdown();
            System.out.println(">>> Done");
        }
    }

    private static Set<IWorker> deployWorkers(final Launcher coordinator, final int size) throws Exception {

        final Set<IWorker> deployed_workers = new HashSet<IWorker>();
        for (int i = 0; i < size; i++) {

            deployed_workers.add(coordinator.deployWorkerOnHost(new HostDescriptor()));
        }

        return deployed_workers;
    }
}

final class SayHelloRemoteJob implements IJobRemote<String> {

    private static final transient long serialVersionUID = -8715065957655698996L;

    @Override
    public String call() throws Exception {

        return NormalOperationTest.HELLO;
    }
}

final class NullPointerExceptionRemoteJob implements IJobRemote<String> {

    private static final long serialVersionUID = 9089082845434872396L;

    @Override
    public String call() throws Exception {

        throw new NullPointerException("test");
    }
}
