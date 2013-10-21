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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Periodically scans a {@link ApplicationNetwork network} for an application-specific change.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class Scanner {

    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final String ENABLED_PROPERTY_NAME = "scanner.enabled";
    protected final PropertyChangeSupport property_change_support;
    private final Duration delay;
    private final AtomicBoolean enabled;
    private final Duration timeout;
    private final Integer id;

    protected Scanner(final Duration delay, final Duration timeout) {

        this.delay = delay;
        this.timeout = timeout;
        enabled = new AtomicBoolean();
        property_change_support = new PropertyChangeSupport(this);
        id = NEXT_ID.getAndIncrement();
    }

    protected Scanner(final Duration delay, final Duration timeout, final boolean enabled) {

        this(delay, timeout);
        setEnabled(enabled);
    }

    /**
     * Scans the {@link ApplicationNetwork network} for a change.
     *
     * @param network the network
     */
    public abstract void scan(ApplicationNetwork network);

    /**
     * Gets the delay between the termination of one scan and the commencement of the next.
     *
     * @return the delay between the termination of one scan and the commencement of the next.
     */
    public Duration getCycleDelay() {

        return delay;
    }

    /**
     * Gets the timeout of a scan cycle.
     *
     * @return the timeout of a scan cycle
     * @see Duration
     */
    public Duration getScanTimeout() {

        return timeout;
    }

    /**
     * Checks if this scanner is enabled.
     *
     * @return {@code true} if this scanner is enabled
     */
    public boolean isEnabled() {

        return enabled.get();
    }

    /**
     * Sets the policy on whether the future scans should be performed.
     * This method has no effect on an executing scan cycle.
     *
     * @param enabled whether to perform scans.
     */
    public void setEnabled(final boolean enabled) {

        final boolean old_enabled = this.enabled.getAndSet(enabled);
        firePropertyChange(ENABLED_PROPERTY_NAME, old_enabled, enabled);
    }

    public void addEnabledPropertyChangeListener(final PropertyChangeListener listener) {

        addPropertyChangeListener(ENABLED_PROPERTY_NAME, listener);
    }

    public void removeEnabledPropertyChangeListener(final PropertyChangeListener listener) {

        removePropertyChangeListener(ENABLED_PROPERTY_NAME, listener);
    }

    @Override
    public int hashCode() {

        return id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {

        return Scanner.class.isInstance(other) && Scanner.class.cast(other).id.equals(id);
    }

    /**
     * Add a {@link PropertyChangeListener} for a specific property.
     * For each property, the listener will be invoked the number of times it was added for that property.
     * If {@code property_name} or listener is {@code null} no exception is thrown and no action is taken.
     *
     * @param property_name The name of the property to listen on
     * @param listener the listener to be added
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    protected void addPropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.addPropertyChangeListener(property_name, listener);
    }

    /**
     * Remove a {@link PropertyChangeListener} for a specific property.
     * If {@code property_name} is null, no exception is thrown and no action is taken.
     * If listener is {@code null} or was never added for the specified property, no exception is thrown and no action is taken.
     *
     * @param property_name The name of the property that was listened on
     * @param listener the listener to be removed
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    protected void removePropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(property_name, listener);
    }

    protected void firePropertyChange(final String property_name, final Object old_value, final Object new_value) {

        property_change_support.firePropertyChange(property_name, old_value, new_value);
    }
}
