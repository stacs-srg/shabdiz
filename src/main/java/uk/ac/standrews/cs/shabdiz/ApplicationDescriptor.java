/*
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
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.HostWrapper;

/**
 * Describes an application.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationDescriptor implements Comparable<ApplicationDescriptor> {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final String STATE_PROPERTY_NAME = "state";
    private final Long id; // used to resolve ties when comparing
    private final Host host;
    private final AtomicReference<ApplicationState> state;
    private final AtomicReference<Object> application_reference;
    private final ConcurrentSkipListSet<Process> processes;
    private final ApplicationManager application_manager;
    protected final PropertyChangeSupport property_change_support;

    public ApplicationDescriptor(final Host host, final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        id = generateId();
        processes = new ConcurrentSkipListSet<Process>(new ProcessHashcodeComparator());
        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
        application_reference = new AtomicReference<Object>();
        property_change_support = new PropertyChangeSupport(this);
        this.host = new HostWrapper(host) {

            @Override
            public Process execute(final String... command) throws IOException {

                final Process process = getUnwrappedHost().execute(command);
                processes.add(process);
                return process;
            }
        };
    }

    public ApplicationManager getApplicationManager() {

        return application_manager;
    }

    public Host getHost() {

        return host;
    }

    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    public ApplicationState getCachedApplicationState() {

        return state.get();
    }

    @SuppressWarnings("unchecked")
    public <T> T getApplicationReference() {

        return (T) application_reference.get();
    }

    protected void setApplicationReference(final Object reference) {

        application_reference.set(reference);
    }

    protected void setCachedApplicationState(final ApplicationState new_state) {

        final ApplicationState old_state = state.getAndSet(new_state);
        property_change_support.firePropertyChange(STATE_PROPERTY_NAME, old_state, new_state);
    }

    /**
     * Adds a {@link PropertyChangeListener} for the {@link #getCachedApplicationState() cached state} property.
     * If listener is {@code null} no exception is thrown and no action is taken.
     * 
     * @param listener the listener to be added
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void addStateChangeListener(final PropertyChangeListener listener) {

        property_change_support.addPropertyChangeListener(STATE_PROPERTY_NAME, listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} for the {@link #getCachedApplicationState() cached state} property.
     * If listener is {@code null} or was never added for the specified property, no exception is thrown and no action is taken.
     * 
     * @param listener the listener to be removed
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void removeStateChangeListener(final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(STATE_PROPERTY_NAME, listener);
    }

    /**
     * Checks if the {@link ApplicationDescriptor#getCachedApplicationState() cached state} of this descriptor is equal to one of the given {@code states}.
     * 
     * @param states the states to check for
     * @return {@code true} if the {@link ApplicationDescriptor#getCachedApplicationState() cached state} of this descriptor is equal to one of the given {@code states}, {@code false} otherwise
     */
    public boolean isInState(final ApplicationState... states) {

        final ApplicationState cached_state = getCachedApplicationState();
        for (final ApplicationState state : states) {
            if (cached_state.equals(state)) { return true; }
        }
        return false;
    }

    /**
     * Causes the current thread to wait until all the {@link ApplicationDescriptor instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states which application instances must reach at least once
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        if (!isInState(states)) {
            final CountDownLatch latch = new CountDownLatch(1);
            final PropertyChangeListener state_change = new SelfRemovingStateChangeListener(this, states, latch);
            addStateChangeListener(state_change);
            latch.await();
        }
    }

    @Override
    public int compareTo(final ApplicationDescriptor other) {

        return id.compareTo(other.id);
    }

    private static Long generateId() {

        return NEXT_ID.getAndIncrement();
    }

    private static final class ProcessHashcodeComparator implements Comparator<Process> {

        @Override
        public int compare(final Process o1, final Process o2) {

            return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
        }
    }
}
