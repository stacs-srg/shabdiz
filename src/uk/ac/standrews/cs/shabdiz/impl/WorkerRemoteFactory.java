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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.JavaProcessDescriptor;
import uk.ac.standrews.cs.nds.madface.ProcessDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.DeploymentException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.nds.p2p.network.InvalidServerClassException;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

import com.mindbright.ssh2.SSH2Exception;

/**
 * A factory for creating {@link IWorkerRemote} objects.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteFactory {

    private static final int INITIAL_PORT = 57496; // First port to attempt when trying to find free port. Note: Chord's start port is 55496, and Trombone's is 56496
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(INITIAL_PORT); // The next port to be used; static to allow multiple concurrent networks.
    private static final Duration OVERALL_TIMEOUT_INTERVAL = new Duration(30, TimeUnit.SECONDS); // Overall timeout for establishing connection to node.
    private static final Duration INDIVIDUAL_TIMEOUT_INTERVAL = new Duration(10, TimeUnit.SECONDS); // Timeout for individual connection attempt.
    private static final Duration RETRY_INTERVAL = new Duration(3, TimeUnit.SECONDS); // Interval between retry of connecting to remote nodes.
    private static final int COORDINATOR_ADDRESS_DEPLOYMENT_PARAM_INDEX = 0;

    /**
     * Instantiates a new worker node factory.
     */
    public WorkerRemoteFactory() {

    }

    /**
     * Creates a new node running at a given network address on a given port, establishing a new one-node ring. The process handle and application reference for the node are established and assigned to the appropriate fields of the host descriptor.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     * @throws InvalidServerClassException the invalid server class exception
     * @throws InterruptedException the interrupted exception
     * @throws UnsupportedPlatformException the unsupported platform exception
     * @throws DeploymentException the deployment exception
     * @throws RPCException the rPC exception
     */
    public void createNode(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException, UnsupportedPlatformException, DeploymentException, RPCException {

        if (host_descriptor.getPort() == 0) {
            createAndBindToNodeOnFreePort(host_descriptor);
        }
        else {
            createAndBindToNodeOnSpecifiedPort(host_descriptor);
        }
    }

    /**
     * Creates a new worker node running in the current JVM at a given local network address on a given port.
     *
     * @param local_address the local address of the node
     * @param coordinator_address the coordinator_address
     * @return the new node
     * @throws IOException if the node cannot bind to the specified local address
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public static IWorkerRemote createNode(final InetSocketAddress local_address, final InetSocketAddress coordinator_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return new WorkerRemote(local_address, coordinator_address);
    }

    /**
     * Binds to an existing remote worker node running at a given network address, checking for liveness.
     *
     * @param worker_address the address of the existing worker
     * @return a remote reference to the node
     *
     * @throws RPCException if an error occurs communicating with the remote machine
     */
    public IWorkerRemote bindToNode(final InetSocketAddress worker_address) throws RPCException {

        final WorkerRemoteProxy worker = WorkerRemoteProxyFactory.getProxy(worker_address);

        worker.ping(); // Check that the remote application can be contacted.

        return worker;
    }

    /**
     * Binds to an existing remote worker running at a given network address, checking for liveness, retrying on any error until the timeout interval has elapsed.
     *
     * @param worker_address the address of the existing worker
     * @param timeout_interval the timeout_interval
     * @param retry_interval the retry_interval
     * @return a remote reference to the node
     * @throws RPCException the rPC exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException if the node cannot be bound to within the timeout interval
     */
    public IWorkerRemote bindToNode(final InetSocketAddress worker_address, final Duration timeout_interval, final Duration retry_interval) throws RPCException, InterruptedException, TimeoutException {

        final Callable<IWorkerRemote> action = new Callable<IWorkerRemote>() {

            @Override
            public IWorkerRemote call() throws Exception {

                return bindToNode(worker_address);
            }
        };

        try {
            return Timing.retry(action, timeout_interval, retry_interval, true, DiagnosticLevel.FULL);
        }
        catch (final Exception e) {
            if (e instanceof InterruptedException) { throw (InterruptedException) e; }
            if (e instanceof TimeoutException) { throw (TimeoutException) e; }
            if (e instanceof RuntimeException) { throw (RuntimeException) e; }

            throw new IllegalStateException("Unexpected checked exception", e);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    private IWorkerRemote bindToNodeWithRetry(final HostDescriptor host_descriptor) throws UnknownHostException, TimeoutException, InterruptedException, RPCException {

        return bindToNode(host_descriptor.getInetSocketAddress(), INDIVIDUAL_TIMEOUT_INTERVAL, RETRY_INTERVAL);
    }

    private List<String> constructArgs(final HostDescriptor host_descriptor, final int port) throws DeploymentException {

        final List<String> args = new ArrayList<String>();

        args.add(WorkerNodeServer.LOCAL_ADDRESS_KEY + NetworkUtil.formatHostAddress(host_descriptor.getHost(), port));

        final InetSocketAddress coordinator_address = getCoordinatorAddress(host_descriptor);
        if (coordinator_address == null) { throw new DeploymentException("argument [coordinator address] is not set"); }

        args.add(WorkerNodeServer.COORDINATOR_ADDRESS_KEY + NetworkUtil.formatHostAddress(coordinator_address.getHostName(), coordinator_address.getPort()));
        args.add(WorkerNodeServer.DIAGNOSTIC_LEVEL_KEY + Diagnostic.getLevel().numericalValue());

        return args;
    }

    private void createAndBindToNodeOnSpecifiedPort(final HostDescriptor host_descriptor) throws SSH2Exception, IOException, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException, UnsupportedPlatformException, DeploymentException, RPCException {

        final List<String> args = constructArgs(host_descriptor, host_descriptor.getPort());

        if (host_descriptor.deployInLocalProcess()) {

            try {

                new WorkerNodeServer(listToArray(args)).createNode();

                // Bind to the node, establishing the application reference.
                host_descriptor.applicationReference(bindToNodeWithRetry(host_descriptor));
            }
            catch (final Exception e) {
                throw new InvalidServerClassException(e);
            }
        }
        else {
            ProcessDescriptor java_process_descriptor = null;
            try {
                // Create a node process.
                java_process_descriptor = new JavaProcessDescriptor().classToBeInvoked(WorkerNodeServer.class).args(args);
                final Process process = host_descriptor.getProcessManager().runProcess(java_process_descriptor);

                host_descriptor.process(process);

                // Bind to the node, establishing the application reference.
                host_descriptor.applicationReference(bindToNodeWithRetry(host_descriptor));
            }
            finally {
                java_process_descriptor.shutdown();
            }
        }
    }

    private String[] listToArray(final List<String> args) {

        return args.toArray(new String[0]);
    }

    private void createAndBindToNodeOnFreePort(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException {

        // TODO have target node select free port and notify back to temporary server.

        final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, OVERALL_TIMEOUT_INTERVAL, true, true, "P2PNodeFactory");

        try {
            timeout_executor.executeWithTimeout(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    while (!Thread.currentThread().isInterrupted()) {

                        final int next = NEXT_PORT.getAndIncrement();
                        host_descriptor.port(next);

                        try {
                            createAndBindToNodeOnSpecifiedPort(host_descriptor);
                            break;
                        }
                        catch (final TimeoutException e) {

                            Diagnostic.trace("timed out trying to connect to port: " + host_descriptor.getPort());
                        }
                    }
                    return null;
                }
            });
        }
        catch (final Exception e) {
            launderException(e);
        }
        finally {
            timeout_executor.shutdown();
        }
    }

    private static void launderException(final Exception e) throws SSH2Exception, IOException, TimeoutException, InvalidServerClassException, UnknownPlatformException, InterruptedException {

        if (e instanceof ExecutionException) {
            launderException((Exception) e.getCause());
        }
        if (e instanceof InterruptedException) { throw (InterruptedException) e; }
        if (e instanceof SSH2Exception) { throw (SSH2Exception) e; }
        if (e instanceof IOException) { throw (IOException) e; }
        if (e instanceof TimeoutException) { throw (TimeoutException) e; }
        if (e instanceof UnknownPlatformException) { throw (UnknownPlatformException) e; }
        if (e instanceof InvalidServerClassException) { throw (InvalidServerClassException) e; }
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        throw new IllegalStateException("Unexpected checked exception", e);
    }

    private static InetSocketAddress getCoordinatorAddress(final HostDescriptor host_descriptor) throws DeploymentException {

        final Object[] args = host_descriptor.getApplicationDeploymentParams();

        if (isParamNull(args, COORDINATOR_ADDRESS_DEPLOYMENT_PARAM_INDEX)) { return null; }

        final Object arg = args[COORDINATOR_ADDRESS_DEPLOYMENT_PARAM_INDEX];
        if (arg instanceof InetSocketAddress) { return (InetSocketAddress) arg; }
        throw new DeploymentException("argument [coordinator address] is not of type InetSocketAddress");
    }

    private static boolean isParamNull(final Object[] args, final int index) {

        return args == null || args.length <= index || args[index] == null;
    }
}