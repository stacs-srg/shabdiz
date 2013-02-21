package uk.ac.standrews.cs.shabdiz.active;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.interfaces.IPingable;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NamingThreadFactory;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.GlobalHostScanner;
import uk.ac.standrews.cs.shabdiz.active.interfaces.SingleHostScanner;

/**
 * Superclass for application manager implementations. The application being managed must implement the {@link IPingable} interface. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class AbstractApplicationManager implements ApplicationManager {

    private static final Duration APPLICATION_CALL_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    private final List<SingleHostScanner> single_scanners = new ArrayList<SingleHostScanner>();
    private final List<GlobalHostScanner> global_scanners = new ArrayList<GlobalHostScanner>();
    private final ExecutorService executor;

    // -------------------------------------------------------------------------------------------------------

    protected AbstractApplicationManager() {

        executor = Executors.newCachedThreadPool(new NamingThreadFactory(getApplicationName() + "_manager_"));
    }

    /**
     * Establishes an application-level reference for the given host descriptor, and updates the host descriptor.
     * 
     * @param host_descriptor the host descriptor
     * @throws Exception if the reference cannot be established
     */
    public abstract void establishApplicationReference(final HostDescriptor host_descriptor) throws Exception;

    protected abstract String guessFragmentOfApplicationProcessName(final HostDescriptor host_descriptor);

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void attemptApplicationCall(final HostDescriptor host_descriptor) throws Exception {

        // This assumes that the application reference implements IPingable, and uses IPingable.ping() to test for application liveness.
        try {
            tryApplicationCall(host_descriptor);
        }
        catch (final Exception e) {

            // Discard cached application reference.
            Diagnostic.trace(DiagnosticLevel.FULL, "discarding application reference for port ", host_descriptor.getPort(), " due to exception: ", e.getClass().getName(), " : ", e.getMessage());
            host_descriptor.applicationReference(null);
            throw e;
        }
    }

    @Override
    public void shutdown() {

        executor.shutdown();
    }

    @Override
    public void killApplication(final HostDescriptor host_descriptor, final boolean kill_all_instances) throws Exception {

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, "killing application on: " + host_descriptor.getHost());

        if (kill_all_instances) {

            // Try to kill all application instances by guessing the format of the process names.
            // Check for the address being null, which it will be if the descriptor represents an invalid host.
            if (host_descriptor.getInetAddress() != null) {
                host_descriptor.killMatchingProcesses(guessFragmentOfApplicationProcessName(host_descriptor));
            }
        }
        else {
            host_descriptor.killProcesses();
        }
    }

    @Override
    public List<SingleHostScanner> getSingleScanners() {

        return single_scanners;
    }

    @Override
    public List<GlobalHostScanner> getGlobalScanners() {

        return global_scanners;
    }

    // -------------------------------------------------------------------------------------------------------

    private void tryApplicationCall(final HostDescriptor host_descriptor) throws InterruptedException, ExecutionException, TimeoutException {

        // Try to connect to the application, subject to a timeout.
        // Use Callable rather than Runnable, even though there's no result, since Callable can throw exceptions.
        final Callable<Void> application_call = new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                // Use cached application reference if present.
                final IPingable cached_reference = host_descriptor.getApplicationReference();

                if (cached_reference != null) {
                    cached_reference.ping();
                }
                else {
                    // Establish a new connection to the application.
                    establishApplicationReference(host_descriptor);
                }

                return null; // Void callable
            }
        };

        executor.submit(application_call).get(APPLICATION_CALL_TIMEOUT.getLength(), APPLICATION_CALL_TIMEOUT.getTimeUnit());
    }
}
