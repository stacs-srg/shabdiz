/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
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
package uk.ac.standrews.cs.nds.madface.interfaces;

import java.util.concurrent.CountDownLatch;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

/**
 * Interface implemented by application-specific scanners.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface IHostScanner {

    /**
     * Returns the size of the thread pool used for scanner checks.
     * @return the size of the thread pool used for scanner checks
     */
    int getThreadPoolSize();

    /**
     * Returns the minimum cycle time between successive scans of the host list.
     * @return the minimum cycle time between successive scans of the host list
     */
    Duration getMinCycleTime();

    /**
     * Returns the label to be used in a user interface toggle for this scanner, or null if none required.
     * @return the label to be used in a user interface toggle
     */
    String getToggleLabel();

    /**
     * Returns the name of the scanner.
     * @return the name of the scanner
     */
    String getName();

    /**
     * Controls whether the scanner is enabled.
     * @param enabled true if the scanner should be enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Tests whether the scanner is enabled.
     * @return true if the scanner is enabled
     */
    boolean isEnabled();

    /**
     * Returns the timeout executor to be used for executing checks.
     * @return the timeout executor to be used for executing checks
     */
    TimeoutExecutor getTimeoutExecutor();

    /**
     * Shuts down the scanner.
     */
    void shutdown();

    /**
     * Called at the end of each cycle through the host list.
     */
    void cycleFinished();

    /**
     * Tells this scanner to synchronize its operation with the other specified scanner. At the end of each cycle this scanner
     * will wait for the other scanner to reach the end of its current cycle.
     * 
     * @param scanner_to_sync_with the scanner to synchronize with
     */
    void syncWith(IHostScanner scanner_to_sync_with);

    /**
     * Returns the latch for the current cycle, which may be used by other scanners to synchronize with this scanner.
     * 
     * @return the latch for the current cycle, or null if this scanner does not allow other scanners to synchronize with it
     */
    CountDownLatch getCycleLatch();
}
