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
package uk.ac.standrews.cs.shabdiz.new_api;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scans a given {@link ApplicationNetwork network} for an application-specific change.
 * 
 * @param <T> the type of {@link ApplicationDescriptor applications} that are maintained by the given {@link ApplicationNetwork network}
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Scanner<T extends ApplicationDescriptor> {

    /**
     * Add a PropertyChangeListener for a specific property.
     * The same listener object may be added more than once.
     * For each property, the listener will be invoked the number of times it was added for that property.
     * If {@code property_name} or listener is {@code null} no exception is thrown and no action is taken.
     * 
     * @param property_name The name of the property to listen on
     * @param listener the listener to be added
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    void addPropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If listener was added more than once to the same event source for the specified property, it will be notified one less time after being removed.
     * If {@code property_name} is null, no exception is thrown and no action is taken.
     * If listener is {@code null} or was never added for the specified property, no exception is thrown and no action is taken.
     * 
     * @param property_name The name of the property that was listened on
     * @param listener the listener to be removed
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    void removePropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Scans the given {@link ApplicationNetwork network} for an application-specific change.
     */
    void scan();

    /**
     * Gets the application network to be scanned by this scanner.
     * 
     * @return the application network to be scanned by this scanner
     */
    ApplicationNetwork<T> getApplicationNetwork();

    /**
     * Gets the delay between the termination of one scan and the commencement of the next.
     * 
     * @return the delay between the termination of one scan and the commencement of the next.
     */
    Duration getCycleDelay();

    /**
     * Gets the timeout of a scan cycle.
     * 
     * @return the timeout of a scan cycle
     * @see Duration
     */
    Duration getScanTimeout();

    /**
     * Sets the policy on whether the future scans should be performed.
     * This method has no effect on an executing scan cycle.
     * 
     * @param enabled whether to perform scans.
     */
    void setEnabled(boolean enabled);

    /**
     * Checks if this scanner is enabled.
     * 
     * @return {@code true} if this scanner is enabled
     */
    boolean isEnabled();

}
