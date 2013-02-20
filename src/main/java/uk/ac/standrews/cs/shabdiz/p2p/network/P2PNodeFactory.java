/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.p2p.network;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.URL;
import uk.ac.standrews.cs.shabdiz.active.exceptions.DeploymentException;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.shabdiz.impl.RemoteJavaProcessBuilder;

/**
 * Superclass for node factory implementations.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class P2PNodeFactory {

    private static final Duration OVERALL_TIMEOUT_INTERVAL = new Duration(60, TimeUnit.SECONDS); // Overall timeout for establishing connection to node.
    protected static final Duration INDIVIDUAL_TIMEOUT_INTERVAL = new Duration(40, TimeUnit.SECONDS); // Timeout for individual connection attempt.
    protected static final Duration RETRY_INTERVAL = new Duration(5, TimeUnit.SECONDS); // Interval between retry of connecting to remote nodes.

    private final Class<?> server_class;
    private Constructor<?> node_server_constructor = null;
    private Method create_node_method = null;

    // -------------------------------------------------------------------------------------------------------

    protected P2PNodeFactory() {

        server_class = getNodeServerClass();

        try {
            node_server_constructor = server_class.getConstructor(String[].class);
            create_node_method = server_class.getMethod("createNode");
        }
        catch (final Exception e) {

            ErrorHandling.hardExceptionError(e, "couldn't initialize P2P node factory");
        }
    }

    // -------------------------------------------------------------------------------------------------------

    protected abstract Object bindToNode(final Object... args) throws RPCException;

    protected abstract Object bindToNode(HostDescriptor host_descriptor) throws UnknownHostException, TimeoutException, InterruptedException;

    protected abstract Object createLocalReference(Object node, Object remote_reference);

    protected abstract Class<?> getNodeServerClass();

    protected abstract AtomicInteger getNextPortContainer();

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new node running at a given network address on a given port, establishing a new one-node ring. The process handle and application reference for the node are established and assigned to the appropriate fields of the host descriptor.
     * 
     * @param host_descriptor a structure containing access details for a remote host
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     * @throws InvalidServerClassException
     * @throws InterruptedException
     * @throws UnsupportedPlatformException
     * @throws DeploymentException
     */
    public void createNode(final HostDescriptor host_descriptor) throws IOException, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException, UnsupportedPlatformException, DeploymentException {

        createNode(host_descriptor, null);
    }

    /**
     * Creates a new node running at a given network address on a given port, with a given key, establishing a new one-node ring. The process handle and application reference for the node are established and assigned to the appropriate fields of the host descriptor.
     * 
     * @param host_descriptor a structure containing access details for a remote host
     * @param key the key of the new node
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     * @throws InvalidServerClassException
     * @throws InterruptedException
     * @throws UnsupportedPlatformException
     * @throws DeploymentException
     */
    public void createNode(final HostDescriptor host_descriptor, final IKey key) throws IOException, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException, UnsupportedPlatformException, DeploymentException {

        if (host_descriptor.getPort() == 0) { // Check whether host descriptor's port is unspecified

            if (host_descriptor.local()) { // Check whether the host descriptor represents the local host

                final int free_local_port = NetworkUtil.findFreeLocalTCPPort(); // Find a free port on the local machine
                host_descriptor.port(free_local_port); // Set the port to the host descriptor
                createAndBindToNodeOnSpecifiedPort(host_descriptor, key); // Create node on the specified port and bind to it
            }
            else {

                createAndBindToNodeOnFreePort(host_descriptor, key);
            }
        }
        else {

            createAndBindToNodeOnSpecifiedPort(host_descriptor, key);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    protected List<String> constructArgs(final HostDescriptor host_descriptor, final IKey key, final int port) throws DeploymentException {

        final List<String> args = new ArrayList<String>();

        args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.getHost(), port));
        args.add("-D" + Diagnostic.getLevel().numericalValue());

        if (key != null) {
            args.add("-x" + key.toString(Key.DEFAULT_RADIX));
        }

        return args;
    }

    protected Object bindToNode(final Duration retry_interval, final Duration timeout_interval, final Object... args) throws TimeoutException, InterruptedException {

        final Callable<Object> action = new Callable<Object>() {

            @Override
            public Object call() throws Exception {

                return bindToNode(args);
            }
        };

        try {
            return Timing.retry(action, timeout_interval, retry_interval, true, DiagnosticLevel.FULL);
        }
        catch (final Exception e) {
            launderException2(e);
            return null;
        }
    }

    private void launderException2(final Exception e) throws TimeoutException, InterruptedException {

        if (e instanceof InterruptedException) { throw (InterruptedException) e; }
        if (e instanceof TimeoutException) { throw (TimeoutException) e; }
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        throw new IllegalStateException("Unexpected checked exception", e);
    }

    // -------------------------------------------------------------------------------------------------------

    private void createAndBindToNodeOnSpecifiedPort(final HostDescriptor host_descriptor, final IKey key) throws IOException, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException, UnsupportedPlatformException, DeploymentException {

        final List<String> args = constructArgs(host_descriptor, key, host_descriptor.getPort());

        if (host_descriptor.deployInLocalProcess()) {
            try {

                final Object node_server = node_server_constructor.newInstance(new Object[]{listToArray(args)});
                final Object node = create_node_method.invoke(node_server);

                // Bind to the node, establishing the application reference.
                final Object remote_reference = bindToNode(host_descriptor);

                host_descriptor.applicationReference(createLocalReference(node, remote_reference));
            }
            catch (final Exception e) {
                throw new InvalidServerClassException(e);
            }
        }
        else {

            final RemoteJavaProcessBuilder java_process_builder = new RemoteJavaProcessBuilder(server_class);

            java_process_builder.addCommandLineArguments(args);
            java_process_builder.addJVMArguments(host_descriptor.getJVMDeploymentParams());

            for (final URL classpath_url : host_descriptor.getApplicationURLs()) {
                java_process_builder.addClasspath(classpath_url.getRealURL());
            }

            final Process process = java_process_builder.start(host_descriptor.getManagedHost());
            final Object node;
            try {
                // Bind to the node, establishing the application reference.
                node = bindToNode(host_descriptor);
            }
            catch (final UnknownHostException e) {
                process.destroy();
                throw e;
            }
            catch (final InterruptedException e) {
                process.destroy();
                throw e;
            }
            catch (final TimeoutException e) {
                process.destroy();
                throw e;
            }
            host_descriptor.applicationReference(node);
            host_descriptor.process(process);
        }
    }

    private Object[] listToArray(final List<String> args) {

        return args.toArray(new String[0]);
    }

    private void createAndBindToNodeOnFreePort(final HostDescriptor host_descriptor, final IKey key) throws IOException, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException {

        // TODO have target node select free port and notify back to temporary server.

        final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, OVERALL_TIMEOUT_INTERVAL, true, true, "P2PNodeFactory");

        try {
            timeout_executor.executeWithTimeout(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    final AtomicInteger next_port_container = getNextPortContainer();

                    while (!Thread.currentThread().isInterrupted()) {

                        final int next = next_port_container.getAndIncrement();
                        host_descriptor.port(next);

                        try {
                            createAndBindToNodeOnSpecifiedPort(host_descriptor, key);
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

    private static void launderException(final Exception e) throws IOException, TimeoutException, InvalidServerClassException, UnknownPlatformException, InterruptedException {

        if (e instanceof ExecutionException) {
            launderException((Exception) e.getCause());
        }
        if (e instanceof InterruptedException) { throw (InterruptedException) e; }
        if (e instanceof IOException) { throw (IOException) e; }
        if (e instanceof TimeoutException) { throw (TimeoutException) e; }
        if (e instanceof UnknownPlatformException) { throw (UnknownPlatformException) e; }
        if (e instanceof InvalidServerClassException) { throw (InvalidServerClassException) e; }
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        throw new IllegalStateException("Unexpected checked exception", e);
    }
}
