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
package uk.ac.standrews.cs.shabdiz.scanners;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;

/**
 * A host scanner that concurrently scans a given collection of hosts.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class ConcurrentHostScanner extends AbstractHostScanner {

    private static final Logger LOGGER = Logger.getLogger(ConcurrentHostScanner.class.getName());

    private final ExecutorService executor;
    private final ReentrantLock check_lock;
    private final List<Future<?>> scheduled_checks;

    private long cycle_start_time;

    protected ConcurrentHostScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time, final Duration check_timeout, final String scanner_name, final boolean enabled) {

        super(manager, min_cycle_time, check_timeout, scanner_name, enabled);
        this.executor = executor;
        check_lock = new ReentrantLock();
        scheduled_checks = new ArrayList<Future<?>>();
    }

    protected abstract void check(HostDescriptor host_descriptor);

    @Override
    public final void scan(final Set<HostDescriptor> host_descriptors) {

        check_lock.lock();
        try {
            if (isEnabled()) {
                prepareForChecks();
                scheduleConcurrentCheckPerHost(host_descriptors);
                awaitCheckCompletion();
                cancelLingeringChecks();
                cycleFinished();
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

    private void scheduleConcurrentCheckPerHost(final Set<HostDescriptor> host_descriptors) {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            final Future<?> future_check = scheduleCheck(host_descriptor);
            scheduled_checks.add(future_check);
        }
    }

    private Future<?> scheduleCheck(final HostDescriptor host_descriptor) {

        return executor.submit(new Runnable() {

            @Override
            public void run() {

                check(host_descriptor);
            }
        });
    }

    private void awaitCheckCompletion() {

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
        final Duration remaining_time = getScanTimeout().subtract(elapsed_time);
        return remaining_time;
    }

    private void cancelLingeringChecks() {

        for (final Future<?> scheduled_check : scheduled_checks) {
            scheduled_check.cancel(true);
        }
    }
}
