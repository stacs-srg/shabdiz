package uk.ac.standrews.cs.shabdiz.coordinator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.ApplicationManager;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.registry.IRegistry;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.registry.stream.RegistryFactory;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.worker.WorkerNodeFactory;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteServer;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

/**
 * Provides management hooks for workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerManager extends ApplicationManager {

    private static final boolean DEFAULT_TRY_REGISTRY_ON_CONNECTION_ERROR = false;

    private static final String APPLICATION_NAME = "Shabdiz Worker";
    private static final String LOCAL_HOSTNAME_SUFFIX = ".local";
    private static final Duration CONNECTION_RETRY = new Duration(5, TimeUnit.SECONDS);
    private static final Duration CONNECTION_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    private final WorkerNodeFactory factory;
    private final boolean try_registry_on_connection_error;

    /**
     * Instantiates a new worker manager which does not try registry on connection error.
     */
    public WorkerManager() {

        this(DEFAULT_TRY_REGISTRY_ON_CONNECTION_ERROR);
    }

    /**
     * Instantiates a new worker manager.
     *
     * @param try_registry_on_connection_error  whether to try to lookup a worker from registry upon connection error
     */
    public WorkerManager(final boolean try_registry_on_connection_error) {

        this.try_registry_on_connection_error = try_registry_on_connection_error;
        factory = new WorkerNodeFactory();
    }

    @Override
    public String getApplicationName() {

        return APPLICATION_NAME;
    }

    @Override
    public void deployApplication(final HostDescriptor host_descriptor) throws Exception {

        factory.createNode(host_descriptor);

        if (host_descriptor.local()) { // Check if deployed locally
            host_descriptor.hostState(HostState.RUNNING); // Since unsuccessful deployment will result in exception, assume deployment was successful and set the host descriptor state directly to speed things up.
        }
    }

    @Override
    public void establishApplicationReference(final HostDescriptor host_descriptor) throws Exception {

        final InetSocketAddress host_address = host_descriptor.getInetSocketAddress();

        if (host_address.getPort() == 0) {

            if (try_registry_on_connection_error) {
                establishApplicationReferenceViaRegistry(host_descriptor, host_address);
            }
            else {
                throw new Exception("trying to establish connection with port 0 and registry retry disabled");
            }
        }
        else {
            try {
                host_descriptor.applicationReference(factory.bindToNode(host_address, CONNECTION_RETRY, CONNECTION_TIMEOUT));
            }
            catch (final Exception e) {

                Diagnostic.trace(DiagnosticLevel.FULL, "giving up establishing reference to: " + host_address);

                if (try_registry_on_connection_error) {
                    establishApplicationReferenceViaRegistry(host_descriptor, host_address);
                }
                else {
                    throw e;
                }
            }
        }

    }

    @Override
    protected String guessFragmentOfApplicationProcessName(final HostDescriptor host_descriptor) {

        final String host_name = stripLocalSuffix(host_descriptor.getInetAddress().getCanonicalHostName());
        return WorkerNodeServer.class.getName() + " -s" + host_name + ":" + host_descriptor.getPort();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void establishApplicationReferenceViaRegistry(final HostDescriptor host_descriptor, final InetSocketAddress inet_socket_address) throws RegistryUnavailableException, RPCException {

        // Try accessing worker server via the registry.
        final InetAddress address = inet_socket_address.getAddress();
        final IRegistry registry = RegistryFactory.FACTORY.getRegistry(address);
        final int worker_remote_server_port = registry.lookup(WorkerRemoteServer.APPLICATION_REGISTRY_KEY);

        host_descriptor.applicationReference(factory.bindToNode(new InetSocketAddress(address, worker_remote_server_port)));
        host_descriptor.port(worker_remote_server_port);
    }

    private String stripLocalSuffix(final String host_name) {

        return host_name.endsWith(LOCAL_HOSTNAME_SUFFIX) ? host_name.substring(0, host_name.length() - LOCAL_HOSTNAME_SUFFIX.length()) : host_name;
    }
}
