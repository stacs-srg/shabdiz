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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.interfaces.HostScanner;

/**
 * Common scanner functionality.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractHostScanner implements HostScanner {

    private final Duration min_cycle_time;

    protected final DefaultMadfaceManager manager;
    protected volatile HostScanner scanner_to_sync_with = null;

    private final AtomicBoolean enabled;
    protected final Duration check_timeout;
    protected final PropertyChangeSupport property_change_support;
    private final String scanner_name;

    protected AbstractHostScanner(final DefaultMadfaceManager manager, final Duration min_cycle_time, final Duration check_timeout, final String scanner_name, final boolean enabled) {

        this.manager = manager;
        this.min_cycle_time = min_cycle_time;
        this.check_timeout = check_timeout;
        this.scanner_name = scanner_name;
        this.enabled = new AtomicBoolean(enabled);
        property_change_support = new PropertyChangeSupport(this);
    }

    @Override
    public Duration getCycleDelay() {

        return min_cycle_time;
    }

    @Override
    public Duration getScanTimeout() {

        return check_timeout;
    }

    @Override
    public boolean isEnabled() {

        return enabled.get();
    }

    @Override
    public void setEnabled(final boolean enabled) {

        this.enabled.set(enabled);
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
    public void syncWith(final HostScanner scanner_to_sync_with) {

        this.scanner_to_sync_with = scanner_to_sync_with;
    }

    /** Override in subclasses that allow other scanners to sync with them. */
    @Override
    public CountDownLatch getCycleLatch() {

        return null;
    }

    @Override
    public void addPropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.addPropertyChangeListener(property_name, listener);
    }

    @Override
    public void removePropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(property_name, listener);
    }

    @Override
    public String getName() {

        return scanner_name;
    }
}
