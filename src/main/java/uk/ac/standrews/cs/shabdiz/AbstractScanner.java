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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.api.Scanner;

/**
 * Common scanner functionality.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractScanner<T extends ApplicationDescriptor> implements Scanner<T> {

    private final Duration delay;
    private final AtomicBoolean enabled;
    private final Duration timeout;
    protected final PropertyChangeSupport property_change_support;
    private volatile ApplicationNetwork<T> application_network;

    protected AbstractScanner(final Duration delay, final Duration timeout, final boolean enabled) {

        this.delay = delay;
        this.timeout = timeout;
        this.enabled = new AtomicBoolean(enabled);
        property_change_support = new PropertyChangeSupport(this);
    }

    void injectApplicationNetwork(final ApplicationNetwork<T> application_network) {

        this.application_network = application_network;
    }

    @Override
    public Duration getCycleDelay() {

        return delay;
    }

    @Override
    public Duration getScanTimeout() {

        return timeout;
    }

    @Override
    public boolean isEnabled() {

        return enabled.get();
    }

    @Override
    public void setEnabled(final boolean enabled) {

        this.enabled.set(enabled);
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
    public void addPropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

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
    public void removePropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(property_name, listener);
    }

    @Override
    public final ApplicationNetwork<T> getApplicationNetwork() {

        return application_network;
    }
}
