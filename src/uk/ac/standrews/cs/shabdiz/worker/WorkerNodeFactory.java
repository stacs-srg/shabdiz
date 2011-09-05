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
package uk.ac.standrews.cs.shabdiz.worker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.DeploymentException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeFactory;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxyFactory;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

/**
 * A factory for creating worker node objects.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNodeFactory extends P2PNodeFactory {

    private static final int INITIAL_PORT = 57496; // First port to attempt when trying to find free port. Note: Chord's start port is 55496, and Trombone's is 56496
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(INITIAL_PORT); // The next port to be used; static to allow multiple concurrent networks.
    protected static final Duration INDIVIDUAL_TIMEOUT_INTERVAL = new Duration(10, TimeUnit.SECONDS); // Timeout for individual connection attempt.
    protected static final Duration RETRY_INTERVAL = new Duration(3, TimeUnit.SECONDS); // Interval between retry of connecting to remote nodes.
    private static final int COORDINATOR_ADDRESS_DEPLOYMENT_PARAM_INDEX = 0;

    /**
     * Instantiates a new worker node factory.
     */
    public WorkerNodeFactory() {

        super();
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
    public IWorkerRemote createNode(final InetSocketAddress local_address, final InetSocketAddress coordinator_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return new Worker(local_address, coordinator_address);
    }

    /**
     * Binds to an existing remote worker node running at a given network address, checking for liveness.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws RPCException if an error occurs communicating with the remote machine
     */
    public IWorkerRemote bindToNode(final InetSocketAddress node_address) throws RPCException {

        final WorkerRemoteProxy worker = WorkerRemoteProxyFactory.getProxy(node_address);

        worker.ping(); // Check that the remote application can be contacted.

        return worker;
    }

    /**
     * Binds to an existing remote worker node running at a given network address, checking for liveness, retrying on any error until the timeout interval has elapsed.
     *
     * @param node_address the address of the existing node
     * @param retry_interval the retry_interval
     * @param timeout_interval the timeout_interval
     * @return a remote reference to the node
     * @throws TimeoutException if the node cannot be bound to within the timeout interval
     * @throws InterruptedException the interrupted exception
     */
    public IWorkerRemote bindToNode(final InetSocketAddress node_address, final Duration retry_interval, final Duration timeout_interval) throws TimeoutException, InterruptedException {

        return (IWorkerRemote) bindToNode(retry_interval, timeout_interval, node_address);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    protected List<String> constructArgs(final HostDescriptor host_descriptor, final IKey key, final int port) throws DeploymentException {

        final List<String> args = new ArrayList<String>();

        args.add(WorkerNodeServer.LOCAL_ADDRESS_KEY + NetworkUtil.formatHostAddress(host_descriptor.getHost(), port));

        final InetSocketAddress coordinator_address = getCoordinatorAddress(host_descriptor);
        if (coordinator_address == null) { throw new DeploymentException("argument [coordinator address] is not set"); }

        args.add(WorkerNodeServer.COORDINATOR_ADDRESS_KEY + NetworkUtil.formatHostAddress(coordinator_address.getHostName(), coordinator_address.getPort()));
        args.add(WorkerNodeServer.DIAGNOSTIC_LEVEL_KEY + Diagnostic.getLevel().numericalValue());

        return args;
    }

    @Override
    protected IWorkerRemote createLocalReference(final Object node, final Object remote_reference) {

        return null;
    }

    @Override
    public Object bindToNode(final Object... args) throws RPCException {

        final InetSocketAddress node_address = (InetSocketAddress) args[0];

        return bindToNode(node_address);
    }

    @Override
    protected Object bindToNode(final HostDescriptor host_descriptor) throws UnknownHostException, TimeoutException, InterruptedException {

        return bindToNode(host_descriptor.getInetSocketAddress(), RETRY_INTERVAL, INDIVIDUAL_TIMEOUT_INTERVAL);
    }

    @Override
    protected Class<?> getNodeServerClass() {

        return WorkerNodeServer.class;
    }

    @Override
    protected AtomicInteger getNextPortContainer() {

        return NEXT_PORT;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

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
