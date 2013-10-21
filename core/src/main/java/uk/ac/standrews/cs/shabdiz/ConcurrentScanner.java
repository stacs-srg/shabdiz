/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
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

package uk.ac.standrews.cs.shabdiz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * A host scanner that concurrently scans each {@link ApplicationDescriptor descriptor} in a given {@link ApplicationNetwork application network}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class ConcurrentScanner extends Scanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentScanner.class);
    private final ReentrantLock scan_lock;
    private final List<Future<?>> scheduled_scans;
    private volatile long cycle_start_time;

    protected ConcurrentScanner(final Duration min_cycle_time, final Duration check_timeout, final boolean enabled) {

        super(min_cycle_time, check_timeout, enabled);
        scan_lock = new ReentrantLock();
        scheduled_scans = new ArrayList<Future<?>>();
    }

    @Override
    public final void scan(final ApplicationNetwork network) {

        scan_lock.lock();
        try {
            beforeScan();
            prepareForScan();
            scheduleConcurrentScans(network);
            awaitScanCompletionUntilTimeoutIsElapsed();
            cancelLingeringScans();
            afterScan();
        }
        finally {
            scan_lock.unlock();
        }
    }

    protected abstract void scan(ApplicationNetwork network, ApplicationDescriptor descriptor);

    /**
     * Method invoked prior to scanning the given network.
     * This implementation does nothing, but may be customised in subclasses.
     * Note: To properly nest multiple overridings, subclasses should generally invoke super.beforeExecute at the end of this method.
     *
     * @see Scanner#scan(ApplicationNetwork)
     */
    protected void beforeScan() {

    }

    /**
     * Method invoked upon successful completion of scanning the given network.
     * This implementation does nothing, but may be customised in subclasses.
     * Note: To properly nest multiple overridings, subclasses should generally invoke super.beforeExecute at the end of this method.
     *
     * @see Scanner#scan(ApplicationNetwork)
     */
    protected void afterScan() {

    }

    private void prepareForScan() {

        cycle_start_time = System.nanoTime();
        scheduled_scans.clear();
    }

    private void scheduleConcurrentScans(final ApplicationNetwork network) {

        for (final ApplicationDescriptor descriptor : network) {
            final Future<?> future_scan = scheduleScan(network, descriptor);
            scheduled_scans.add(future_scan);
        }
    }

    private Future<?> scheduleScan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        final ExecutorService executor = network.getConcurrentScannerExecutor();
        return executor.submit(new Runnable() {

            @Override
            public void run() {

                if (isEnabled()) {
                    try {
                        scan(network, descriptor);
                    }
                    catch (Throwable e) {
                        LOGGER.error("failed to scan descriptor {} of network {} in scanner {}", descriptor, network, this);
                        LOGGER.error("failure occured while scanning descriptor " + descriptor, e);
                    }
                }
            }
        });
    }

    private void awaitScanCompletionUntilTimeoutIsElapsed() {

        for (final Future<?> scheduled_scan : scheduled_scans) {

            final Duration remaining_time = getRemainingTime();
            try {
                scheduled_scan.get(remaining_time.getLength(), remaining_time.getTimeUnit());
            }
            catch (final InterruptedException e) {
                break;
            }
            catch (final TimeoutException e) {
                break;
            }
            catch (final CancellationException e) {
                LOGGER.warn("scheduled host check was cancelled", e);
            }
            catch (final ExecutionException e) {
                LOGGER.warn("schedule host check failed", e.getCause());
            }
        }
    }

    private Duration getRemainingTime() {

        return isEnabled() ? getScanTimeout().subtract(Duration.elapsedNano(cycle_start_time)) : Duration.ZERO;
    }

    private void cancelLingeringScans() {

        for (final Future<?> scheduled_scan : scheduled_scans) {
            scheduled_scan.cancel(true);
        }
    }
}
