/*
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/**
 * THe entry point to start up a new worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerMain {

    public static final String WORKER_REMOTE_ADDRESS_KEY = "WORKER_REMOTE_ADDRESS";
    private static final Logger LOGGER = Logger.getLogger(WorkerMain.class.getName());
    private static final String LOCAL_ADDRESS_KEY = "-s";
    private static final String CALLBACK_ADDRESS_KEY = "-c";
    private static final String THREAD_POOL_SIZE_KEY = "-t";
    private static final String SOCKET_READ_TIMEOUT = "-p";

    private InetSocketAddress local_address = null;
    private InetSocketAddress launcher_callback_address = null;
    private int thread_pool_size;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new worker node server.
     * 
     * @param args the startup arguments
     * @throws UndefinedDiagnosticLevelException the undefined diagnostic level exception
     * @throws UnknownHostException the unknown host exception
     * @throws NumberFormatException if the given thread pool size cannot be converted to an integer value
     */
    public WorkerMain(final String[] args) throws UndefinedDiagnosticLevelException, UnknownHostException {

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);
        configureLocalAddress(arguments);
        configureLauncherAddress(arguments);
        //        configureThreadPoolSize(arguments);
        //        configureSocketReadTimeout(arguments);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the worker service should be made available.</dd>
     * <dt>-chost:port (required)</dt>
     * <dd>Specifies the address and port of an existing launcher callback server.</dd>
     * <dt>-Dlevel (optional)</dt>
     * <dd>Specifies a diagnostic level from 0 (most detailed) to 6 (least detailed).</dd>
     * </dl>
     * 
     * @param args see above
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws UndefinedDiagnosticLevelException if the specified diagnostic level is not valid
     * @throws IOException if a node cannot be created using the given local address
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public static void main(final String[] args) throws RPCException, UndefinedDiagnosticLevelException, IOException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        final WorkerMain server = new WorkerMain(args);
        try {
            final DefaultWorkerRemote worker = server.createNode();
            printWorkerAddress(worker);
            LOGGER.info("Started Shabdiz worker at " + worker.getAddress());
        }
        catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't start Shabdiz worker at " + server.local_address, e);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    private static void printWorkerAddress(final DefaultWorkerRemote worker) {

        ProcessUtil.printKeyValue(System.out, WORKER_REMOTE_ADDRESS_KEY, worker.getAddress());
    }

    public static List<String> constructCommandLineArguments(final InetSocketAddress callback_address, final Integer port, final Integer thread_pool_size, final Duration socket_read_timeout) {

        final List<String> arguments = new ArrayList<String>();
        arguments.add(CALLBACK_ADDRESS_KEY + NetworkUtil.formatHostAddress(callback_address));

        if (port != null) {
            arguments.add(LOCAL_ADDRESS_KEY + NetworkUtil.formatHostAddress("", port));
        }
        if (thread_pool_size != null) {
            arguments.add(THREAD_POOL_SIZE_KEY + thread_pool_size);
        }
        if (socket_read_timeout != null) {
            arguments.add(SOCKET_READ_TIMEOUT + socket_read_timeout);
        }
        return arguments;
    }

    /**
     * Creates a new worker node.
     * 
     * @return the created worker node
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws RPCException the rPC exception
     * @throws AlreadyBoundException the already bound exception
     * @throws RegistryUnavailableException the registry unavailable exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    public DefaultWorkerRemote createNode() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return new DefaultWorkerRemote(local_address, launcher_callback_address);
    }

    // -------------------------------------------------------------------------------------------------------

    private void usage() {

        ErrorHandling.hardError("Usage: -Chost:port [-Shost:port -Tthread_pool_size]");
    }

    private void configureLocalAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String local_address_parameter = arguments.get(LOCAL_ADDRESS_KEY);
        final int port = local_address_parameter != null ? NetworkUtil.extractPortNumber(local_address_parameter) : 0;
        local_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);
    }

    private void configureLauncherAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get(CALLBACK_ADDRESS_KEY);
        if (known_address_parameter == null) {
            usage();
        }
        launcher_callback_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);

    }

    private void configureThreadPoolSize(final Map<String, String> arguments) throws UnknownHostException {

        final String size_as_string = arguments.get(THREAD_POOL_SIZE_KEY);
        if (size_as_string != null) {
            thread_pool_size = Integer.parseInt(size_as_string);
        }
    }

    private void configureSocketReadTimeout(final Map<String, String> arguments) throws UnknownHostException {

        final String socket_read_timeout = arguments.get(CALLBACK_ADDRESS_KEY);
        if (socket_read_timeout != null) {
            StreamProxy.CONNECTION_POOL.setSocketReadTimeout(Duration.valueOf(socket_read_timeout));
        }
    }
}
