/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active.scanners;

import java.util.concurrent.CountDownLatch;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.active.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IHostScanner;

/**
 * Common scanner functionality.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class Scanner implements IHostScanner {

    private final int thread_pool_size;
    private final Duration min_cycle_time;
    private final TimeoutExecutor timeout_executor;

    protected final MadfaceManager manager;
    protected volatile IHostScanner scanner_to_sync_with = null;

    protected volatile boolean enabled;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes the scanner.
     * 
     * @param manager the manager
     * @param min_cycle_time the minimum time between successive cycles
     * @param thread_pool_size the thread pool size for status scan checks
     * @param check_timeout the timeout for attempted checks
     * @param scanner_name the name of the scanner
     * @param enabled true if the scanner is initially enabled
     */
    public Scanner(final MadfaceManager manager, final Duration min_cycle_time, final int thread_pool_size, final Duration check_timeout, final String scanner_name, final boolean enabled) {

        this.manager = manager;

        this.thread_pool_size = thread_pool_size;
        this.min_cycle_time = min_cycle_time;
        this.enabled = enabled;

        timeout_executor = TimeoutExecutor.makeTimeoutExecutor(thread_pool_size, check_timeout, false, false, scanner_name);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns the size of the thread pool used for scanner checks.
     * @return the size of the thread pool used for scanner checks
     */
    @Override
    public int getThreadPoolSize() {

        return thread_pool_size;
    }

    @Override
    public Duration getMinCycleTime() {

        return min_cycle_time;
    }

    @Override
    public boolean isEnabled() {

        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {

        this.enabled = enabled;
    }

    @Override
    public TimeoutExecutor getTimeoutExecutor() {

        return timeout_executor;
    }

    @Override
    public void shutdown() {

        timeout_executor.shutdown();
    }

    @Override
    public void cycleFinished() {

        if (scanner_to_sync_with != null) {
            try {
                scanner_to_sync_with.getCycleLatch().await();
            }
            catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void syncWith(final IHostScanner scanner_to_sync_with) {

        this.scanner_to_sync_with = scanner_to_sync_with;
    }

    /**
     * Override in subclasses that allow other scanners to sync with them.
     */
    @Override
    public CountDownLatch getCycleLatch() {

        return null;
    }
}
