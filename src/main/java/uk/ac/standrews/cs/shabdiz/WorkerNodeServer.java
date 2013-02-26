/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
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
package uk.ac.standrews.cs.shabdiz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;

/**
 * THe entry point to start up a new worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNodeServer {

    private static final String WORKER_REMOTE_ADDRESS_KEY = "WORKER_REMOTE_ADDRESS=";

    private static final Logger LOGGER = Logger.getLogger(WorkerNodeServer.class.getName());

    /** The parameter that specifies the diagnostic level. */
    public static final String DIAGNOSTIC_LEVEL_KEY = "-D";

    /** The parameter that specifies the local address. */
    public static final String LOCAL_ADDRESS_KEY = "-s";

    /** The parameter that specifies the launcher callback address. */
    public static final String LAUNCHER_CALLBACK_ADDRESS_KEY = "-c";

    /** The parameter that specifies the thread pool size of local executor. */
    public static final String THREAD_POOL_SIZE_KEY = "-t";

    private static final String DIAGNOSTIC_DATE_FORMAT = "HH:mm:ss:SSS ";
    private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.NONE;
    private static final Duration WORKER_SOCKET_READ_TIMEOUT = new Duration(50, TimeUnit.SECONDS);

    private InetSocketAddress local_address = null;
    private InetSocketAddress launcher_callback_address = null;

    // -------------------------------------------------------------------------------------------------------

    static {
        StreamProxy.CONNECTION_POOL.setSocketReadTimeout(WORKER_SOCKET_READ_TIMEOUT);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new worker node server.
     * 
     * @param args the startup arguments
     * @throws UndefinedDiagnosticLevelException the undefined diagnostic level exception
     * @throws UnknownHostException the unknown host exception
     * @throws NumberFormatException if the given thread pool size cannot be converted to an integer value
     */
    public WorkerNodeServer(final String[] args) throws UndefinedDiagnosticLevelException, UnknownHostException {

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);

        configureLogLevel(arguments);
        configureLocalAddress(arguments);
        configureLauncherAddress(arguments);
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

        final WorkerNodeServer server = new WorkerNodeServer(args);
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

        synchronized (System.out) {
            System.out.println(WORKER_REMOTE_ADDRESS_KEY + worker.getAddress());
        }
    }

    public static InetSocketAddress parseOutputLine(final String line) throws UnknownHostException {

        return line != null && line.startsWith(WORKER_REMOTE_ADDRESS_KEY) ? Marshaller.getAddress(line.split("=")[1]) : null;
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

        ErrorHandling.hardError("Usage: -shost:port -chost:port [-Dlevel]");
    }

    private void configureLogLevel(final Map<String, String> arguments) throws UndefinedDiagnosticLevelException {

        Diagnostic.setLevel(DiagnosticLevel.getDiagnosticLevelFromCommandLineArg(arguments.get(DIAGNOSTIC_LEVEL_KEY), DEFAULT_DIAGNOSTIC_LEVEL));
        Diagnostic.setTimestampFlag(true);
        Diagnostic.setTimestampFormat(new SimpleDateFormat(DIAGNOSTIC_DATE_FORMAT));
        Diagnostic.setTimestampDelimiterFlag(false);
        ErrorHandling.setTimestampFlag(false);
    }

    private void configureLocalAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String local_address_parameter = arguments.get(LOCAL_ADDRESS_KEY);
        if (local_address_parameter != null) {
            local_address = NetworkUtil.extractInetSocketAddress(local_address_parameter, 0);
        }
        else {
            local_address = NetworkUtil.getLocalIPv4InetSocketAddress(0);
        }
    }

    private void configureLauncherAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get(LAUNCHER_CALLBACK_ADDRESS_KEY);
        if (known_address_parameter == null) {
            usage();
        }
        launcher_callback_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);

    }
}
