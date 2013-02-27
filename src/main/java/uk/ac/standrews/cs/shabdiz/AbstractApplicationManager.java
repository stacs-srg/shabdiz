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

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NamingThreadFactory;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.Scanner;
import uk.ac.standrews.cs.shabdiz.util.URL;

/**
 * Superclass for application manager implementations. The application being managed must implement the {@link Pingable} interface. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractApplicationManager implements ApplicationManager {

    private static final Duration APPLICATION_CALL_TIMEOUT = new Duration(15, TimeUnit.SECONDS);

    private final List<Scanner> multiple_host_scanners = new ArrayList<Scanner>();
    private final ExecutorService executor;

    private Set<URL> application_urls;

    protected AbstractApplicationManager() {

        application_urls = Collections.synchronizedSet(new HashSet<URL>());
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

    @Override
    public void attemptApplicationCall(final HostDescriptor host_descriptor) throws Exception {

        // This assumes that the application reference implements IPingable, and uses IPingable.ping() to test for application liveness.
        try {
            tryApplicationCall(host_descriptor);
        }
        catch (final Exception e) {

            // Discard cached application reference.
            Diagnostic.trace(DiagnosticLevel.FULL, "discarding application reference for port ", host_descriptor.getPort(), " due to exception: ", e.getClass().getName(), " : ", e.getMessage());
            host_descriptor.discardApplicationReference();
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
    public List<Scanner> getScanners() {

        return multiple_host_scanners;
    }

    @Override
    public Set<URL> getApplicationLibraryURLs() {

        return application_urls;
    }

    @Override
    public void setApplicationLibraryURLs(final Set<URL> urls) {

        application_urls = urls;

    }

    private Class<? extends ApplicationManager> getApplicationManagerClass(final String application_entrypoint_name) throws ClassNotFoundException {

        final ClassLoader parent_loader = Thread.currentThread().getContextClassLoader();
        final java.net.URL[] url_array = URL.toArrayAsRealURLs(application_urls);
        final ClassLoader url_class_loader = new URLClassLoader(url_array, parent_loader);

        return (Class<? extends ApplicationManager>) Class.forName(application_entrypoint_name, true, url_class_loader);
    }

    // -------------------------------------------------------------------------------------------------------

    private void tryApplicationCall(final HostDescriptor host_descriptor) throws InterruptedException, ExecutionException, TimeoutException {

        // Try to connect to the application, subject to a timeout.
        // Use Callable rather than Runnable, even though there's no result, since Callable can throw exceptions.
        final Callable<Void> application_call = new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                // Use cached application reference if present.
                final Pingable cached_reference = host_descriptor.getApplicationReference();

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
