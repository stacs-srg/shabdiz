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
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.WorkerNodeFactory;

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

    /** The parameter that specifies the coordinator address. */
    public static final String COORDINATOR_ADDRESS_KEY = "-c";

    private static final String DIAGNOSTIC_DATE_FORMAT = "HH:mm:ss:SSS ";
    private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.NONE;
    private static final Duration WORKER_SOCKET_READ_TIMEOUT = new Duration(20, TimeUnit.SECONDS);

    private static final WorkerNodeFactory FACTORY;

    private InetSocketAddress local_address = null;
    private InetSocketAddress coordinator_address = null;

    // -------------------------------------------------------------------------------------------------------

    static {
        FACTORY = new WorkerNodeFactory();
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
        configureCoordinatorAddress(arguments);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the Shabdiz Worker service should be made available.</dd>
     * 
     * <dt>-khost:port (optional)</dt>
     * <dd>Specifies the address and port of an existing Shabdiz coordinator, by which the new node is coordinated.</dd>
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

        return FACTORY.createNode(local_address, coordinator_address);
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

    private void configureCoordinatorAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get(COORDINATOR_ADDRESS_KEY);
        if (known_address_parameter == null) {
            usage();
        }
        coordinator_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);
    }
}
