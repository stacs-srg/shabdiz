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
package uk.ac.standrews.cs.shabdiz.worker.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;
import uk.ac.standrews.cs.shabdiz.impl.WorkerRemoteFactory;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;

/**
 * THe entry point to start up a new worker.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNodeServer {

    /** The parameter that specifies the diagnostic level. */
    public static final String DIAGNOSTIC_LEVEL_KEY = "-D";

    /** The parameter that specifies the local address. */
    public static final String LOCAL_ADDRESS_KEY = "-s";

    /** The parameter that specifies the launcher callback address. */
    public static final String LAUNCHER_CALLBACK_ADDRESS_KEY = "-c";

    private static final String DIAGNOSTIC_DATE_FORMAT = "HH:mm:ss:SSS ";
    private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.NONE;
    private static final Duration WORKER_SOCKET_READ_TIMEOUT = new Duration(20, TimeUnit.SECONDS);

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
     */
    public WorkerNodeServer(final String[] args) throws UndefinedDiagnosticLevelException, UnknownHostException {

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);

        configureDiagnostics(arguments);
        configureLocalAddress(arguments);
        configureLauncherAddress(arguments);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the Shabdiz Worker service should be made available.</dd>
     * 
     * <dt>-khost:port (optional)</dt>
     * <dd>Specifies the address and port of an existing Shabdiz launcher callback server.</dd>
     * 
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

            server.createNode();
            Diagnostic.trace("Started Shabdiz worker node at " + server.local_address);
        }
        catch (final IOException e) {
            Diagnostic.trace("Couldn't start Shabdiz worker node at " + server.local_address + " : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------------------------------------

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
    public IWorkerRemote createNode() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return WorkerRemoteFactory.createNode(local_address, launcher_callback_address);
    }

    // -------------------------------------------------------------------------------------------------------

    private void usage() {

        ErrorHandling.hardError("Usage: -shost:port -chost:port [-Dlevel]");
    }

    private void configureDiagnostics(final Map<String, String> arguments) throws UndefinedDiagnosticLevelException {

        Diagnostic.setLevel(DiagnosticLevel.getDiagnosticLevelFromCommandLineArg(arguments.get(DIAGNOSTIC_LEVEL_KEY), DEFAULT_DIAGNOSTIC_LEVEL));
        Diagnostic.setTimestampFlag(true);
        Diagnostic.setTimestampFormat(new SimpleDateFormat(DIAGNOSTIC_DATE_FORMAT));
        Diagnostic.setTimestampDelimiterFlag(false);
        ErrorHandling.setTimestampFlag(false);
    }

    private void configureLocalAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String local_address_parameter = arguments.get(LOCAL_ADDRESS_KEY);
        if (local_address_parameter == null) {
            usage();
        }
        local_address = NetworkUtil.extractInetSocketAddress(local_address_parameter, 0);
    }

    private void configureLauncherAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get(LAUNCHER_CALLBACK_ADDRESS_KEY);
        if (known_address_parameter == null) {
            usage();
        }
        launcher_callback_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);

    }
}
