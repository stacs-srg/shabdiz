/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.interfaces.IApplicationManager;
import uk.ac.standrews.cs.nds.madface.interfaces.IGlobalHostScanner;
import uk.ac.standrews.cs.nds.madface.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.nds.rpc.interfaces.IPingable;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

/**
 * Superclass for application manager implementations. The application being managed must implement the {@link IPingable} interface. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class ApplicationManager implements IApplicationManager {

    private static final Duration APPLICATION_CALL_TIMEOUT = new Duration(60, TimeUnit.SECONDS);

    private static final int APPLICATION_CALL_THREADS = 10;

    private final List<ISingleHostScanner> single_scanners = new ArrayList<ISingleHostScanner>();
    private final List<IGlobalHostScanner> global_scanners = new ArrayList<IGlobalHostScanner>();
    private final TimeoutExecutor executor = TimeoutExecutor.makeTimeoutExecutor(APPLICATION_CALL_THREADS, APPLICATION_CALL_TIMEOUT, true, false, "ApplicationManager");

    // -------------------------------------------------------------------------------------------------------

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
                host_descriptor.killProcesses(guessFragmentOfApplicationProcessName(host_descriptor));
            }
        }
        else {
            host_descriptor.killProcesses();
        }
    }

    @Override
    public List<ISingleHostScanner> getSingleScanners() {

        return single_scanners;
    }

    @Override
    public List<IGlobalHostScanner> getGlobalScanners() {

        return global_scanners;
    }

    // -------------------------------------------------------------------------------------------------------

    private void tryApplicationCall(final HostDescriptor host_descriptor) throws Exception {

        // Try to connect to the application, subject to a timeout.
        // Use Callable rather than Runnable, even though there's no result, since Callable can throw exceptions.
        final Callable<Void> application_call = new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                // Use cached application reference if present.
                final Object cached_reference = host_descriptor.getApplicationReference();

                if (cached_reference != null) {
                    ((IPingable) cached_reference).ping();
                }
                else {
                    // Establish a new connection to the application.
                    establishApplicationReference(host_descriptor);
                }

                return null;
            }
        };

        executor.executeWithTimeout(application_call);
    }
}
