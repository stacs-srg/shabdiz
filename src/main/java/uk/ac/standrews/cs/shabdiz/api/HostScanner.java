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
package uk.ac.standrews.cs.shabdiz.api;

import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;

/**
 * Interface implemented by application-specific scanners.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface HostScanner {

    /**
     * Adds the property change listener.
     * 
     * @param property_name the property name to listen for
     * @param listener the listener
     */
    void addPropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Removes the property change listener.
     * 
     * @param property_name the property name to listen for
     * @param listener the listener
     */
    void removePropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Performs some application-specific global check of the specified hosts.
     * 
     * @param host_state_list the hosts to be checked
     */
    void scan(Set<HostDescriptor> host_state_list);

    /**
     * Returns the minimum cycle time between successive scans of the host list.
     * 
     * @return the minimum cycle time between successive scans of the host list
     */
    Duration getCycleDelay();

    /**
     * Gets the check timeout.
     * 
     * @return the check timeout
     */
    Duration getScanTimeout();

    /**
     * Returns the label to be used in a user interface toggle for this scanner, or null if none required.
     * 
     * @return the label to be used in a user interface toggle
     */
    String getToggleLabel();

    /**
     * Returns the name of the scanner.
     * 
     * @return the name of the scanner
     */
    String getName();

    /**
     * Controls whether the scanner is enabled.
     * 
     * @param enabled true if the scanner should be enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Tests whether the scanner is enabled.
     * 
     * @return true if the scanner is enabled
     */
    boolean isEnabled();

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
    void syncWith(HostScanner scanner_to_sync_with);

    /**
     * Returns the latch for the current cycle, which may be used by other scanners to synchronize with this scanner.
     * 
     * @return the latch for the current cycle, or null if this scanner does not allow other scanners to synchronize with it
     */
    CountDownLatch getCycleLatch();
}
