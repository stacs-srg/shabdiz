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
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * A host scanner that concurrently scans each {@link ApplicationDescriptor descriptor} in a given {@link ApplicationNetwork application network}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractConcurrentScanner extends AbstractScanner {

    private static final Logger LOGGER = Logger.getLogger(AbstractConcurrentScanner.class.getName());

    private final ReentrantLock check_lock;
    private final List<Future<?>> scheduled_checks;
    private volatile ExecutorService executor;
    private volatile long cycle_start_time;

    protected AbstractConcurrentScanner(final Duration min_cycle_time, final Duration check_timeout, final boolean enabled) {

        super(min_cycle_time, check_timeout, enabled);
        check_lock = new ReentrantLock();
        scheduled_checks = new ArrayList<Future<?>>();
    }

    protected abstract void scan(ApplicationNetwork network, ApplicationDescriptor descriptor);

    void injectExecutorService(final ExecutorService executor) {

        this.executor = executor;
    }

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

    @Override
    public final void scan(final ApplicationNetwork network) {

        check_lock.lock();
        try {
            if (isEnabled()) {
                beforeScan();
                prepareForChecks();
                scheduleConcurrentChecks(network);
                awaitCheckCompletionUntilTimeoutIsElapsed();
                cancelLingeringChecks();
                afterScan();
            }
        }
        finally {
            check_lock.unlock();
        }
    }

    private void prepareForChecks() {

        cycle_start_time = System.nanoTime();
        scheduled_checks.clear();
    }

    private void scheduleConcurrentChecks(final ApplicationNetwork network) {

        for (final ApplicationDescriptor descriptor : network) {
            final Future<?> future_check = scheduleCheck(network, descriptor);
            scheduled_checks.add(future_check);
        }
    }

    private Future<?> scheduleCheck(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        return executor.submit(new Runnable() {

            @Override
            public void run() {

                scan(network, descriptor);
            }
        });
    }

    private void awaitCheckCompletionUntilTimeoutIsElapsed() {

        for (final Future<?> scheduled_check : scheduled_checks) {

            final Duration remaining_time = getRemainingTime();
            try {
                scheduled_check.get(remaining_time.getLength(), remaining_time.getTimeUnit());
            }
            catch (final InterruptedException e) {
                break;
            }
            catch (final TimeoutException e) {
                break;
            }
            catch (final CancellationException e) {
                LOGGER.log(Level.WARNING, "schedule host check was cancelled", e);
            }
            catch (final ExecutionException e) {
                LOGGER.log(Level.WARNING, "schedule host check failed", e.getCause());
            }
        }
    }

    private Duration getRemainingTime() {

        final Duration elapsed_time = Duration.elapsedNano(cycle_start_time);
        return getScanTimeout().subtract(elapsed_time);
    }

    private void cancelLingeringChecks() {

        for (final Future<?> scheduled_check : scheduled_checks)
            scheduled_check.cancel(true);
    }
}
