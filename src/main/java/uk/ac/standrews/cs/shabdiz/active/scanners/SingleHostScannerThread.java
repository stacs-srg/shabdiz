/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active.scanners;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ISingleHostScanner;

/**
 * Thread that continually monitors the status of the hosts in a given list, the contents of which may vary dynamically.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class SingleHostScannerThread extends HostScannerThread {

    // -------------------------------------------------------------------------------------------------------

    private final ISingleHostScanner scanner;
    private final Set<IAttributesCallback> attributes_callbacks;
    private final boolean debug;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a monitor for the host list.
     * 
     * @param manager the manager
     * @param scanner an application-specific scanner
     */
    public SingleHostScannerThread(final MadfaceManager manager, final ISingleHostScanner scanner) {

        super(scanner);
        host_state_list = manager.getHostDescriptors();
        attributes_callbacks = manager.getAttributesCallbacks();
        this.scanner = scanner;
        debug = scanner.getName().equals("Status") && !"masih".equals(System.getenv("USER"));
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void run() {

        final TimeoutExecutor executor = scanner.getTimeoutExecutor();
        final Duration scanner_min_cycle_time = scanner.getMinCycleTime();

        // Task to perform the appropriate check of all the known hosts.
        // Checks are performed concurrently as decided by the scanner's executor.
        final Callable<Void> check_all_hosts = new Callable<Void>() {

            int count = 1;

            /**
             * First, for each known host, queues a task to check it. Then, waits for all those tasks to complete or time out.
             */
            @Override
            public Void call() throws Exception {

                if (scanner.isEnabled()) {

                    if (debug) {
                        System.out.println(">>>> starting status scanner check: " + count);
                    }
                    final Duration start = Duration.elapsed();

                    // Don't know before the loop how many host descriptors will be visited by the iterator, because the host set is a concurrent data structure.
                    // Even if we check its size now we're not guaranteed to visit that number of entries. So delay creation of count-down latch until known.
                    final AtomicReference<CountDownLatch> all_hosts_check_completed_indirection = new AtomicReference<CountDownLatch>();

                    final CountDownLatch indirection_initialized = new CountDownLatch(1);

                    int host_count = 0;

                    for (final HostDescriptor host_descriptor : host_state_list) {

                        host_count++;
                        final Runnable check = makeCheckHostAction(host_descriptor, all_hosts_check_completed_indirection, indirection_initialized);

                        try {
                            executor.executeWithTimeout(check);
                        }
                        catch (final InterruptedException e) {
                            throw e;
                        }
                        catch (final Exception e) {
                            Diagnostic.trace(DiagnosticLevel.FULL, "error in scanner check: " + scanner.getAttributeName() + " : " + e.getClass().getName() + " : " + e.getMessage());
                        }
                    }

                    final CountDownLatch all_hosts_check_completed = new CountDownLatch(host_count);

                    all_hosts_check_completed_indirection.set(all_hosts_check_completed);
                    indirection_initialized.countDown();
                    all_hosts_check_completed.await();

                    if (debug) {
                        System.out.println(">>>> finishing status scanner check: " + count);
                        System.out.println(">>>> elapsed: " + Duration.elapsed(start).convertTo(TimeUnit.SECONDS));
                        count++;
                    }
                }

                scanner.cycleFinished();

                return null;
            }
        };

        try {
            // No fixed delay between iterations.
            Timing.repeat(check_all_hosts, Duration.MAX_DURATION, scanner_min_cycle_time, false, DiagnosticLevel.NONE);
        }
        catch (final InterruptedException e) {
            Diagnostic.trace(DiagnosticLevel.FULL, "scanner: " + scanner.getAttributeName() + " interrupted");
        }
        catch (final TimeoutException e) {
            Diagnostic.trace("scanner: " + scanner.getAttributeName() + " timed out unexpectedly");
        }
        catch (final Exception e) {
            throw new IllegalStateException("Unexpected checked exception", e);
        }
        catch (final Throwable t) {
            System.err.println("******* host scanner thread terminating due to error *********");
            System.err.println(scanner.getName());
        }
    }

    private Runnable makeCheckHostAction(final HostDescriptor host_descriptor, final AtomicReference<CountDownLatch> all_hosts_check_completed_indirection, final CountDownLatch indirection_initialized) {

        return new Runnable() {

            @Override
            public void run() {

                try {
                    scanner.check(host_descriptor, attributes_callbacks);
                }

                catch (final Exception e) {
                    ErrorHandling.error("inner error in scanner check: ", e.getClass().getName(), " : ", e.getMessage());
                    e.printStackTrace();
                    final Throwable cause = e.getCause();
                    if (cause != null) {
                        ErrorHandling.error("caused by: ", cause.getClass().getName(), " : ", cause.getMessage());
                    }
                }
                finally {
                    try {
                        indirection_initialized.await();
                        final CountDownLatch all_hosts_check_completed = all_hosts_check_completed_indirection.get();
                        if (all_hosts_check_completed != null) {
                            all_hosts_check_completed.countDown();
                        }
                    }
                    catch (final InterruptedException e) {
                        Diagnostic.trace("interrupted while waiting for latch indirection to be initialized");
                    }
                }
            }
        };
    }
}
