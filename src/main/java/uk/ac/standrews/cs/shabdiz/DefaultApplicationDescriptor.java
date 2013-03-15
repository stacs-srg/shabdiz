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
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.host.HostWrapper;

public class DefaultApplicationDescriptor implements ApplicationDescriptor, Comparable<DefaultApplicationDescriptor> {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final Long id; // used to resolve ties when comparing
    private final Host host;
    private final AtomicReference<ApplicationState> state;
    protected final PropertyChangeSupport property_change_support;
    private static final String STATE_PROPERTY_NAME = "state";
    private final ConcurrentSkipListSet<Process> processes;
    private final ApplicationManager application_manager;

    public DefaultApplicationDescriptor(final Host host, final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        id = generateId();
        processes = new ConcurrentSkipListSet<Process>(new ProcessHashcodeComparator());
        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
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

    @Override
    public ApplicationManager getApplicationManager() {

        return application_manager;
    }

    @Override
    public Host getHost() {

        return host;
    }

    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    @Override
    public ApplicationState getCachedApplicationState() {

        return state.get();
    }

    @Override
    public void setCachedApplicationState(final ApplicationState new_state) {

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
     * Checks if a given {@link ApplicationDescriptor} is in one of the given states.
     * 
     * @param states the states to check for
     * @return true, if the given {@link ApplicationDescriptor} is in on of the given states
     */
    public boolean isInState(final ApplicationState... states) {

        final ApplicationState cached_state = getCachedApplicationState();
        for (final ApplicationState state : states) {
            if (cached_state.equals(state)) { return true; }
        }
        return false;
    }

    public void kill() {

        final Iterator<Process> process_iterator = processes.iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
        }
    }

    @Override
    public int compareTo(final DefaultApplicationDescriptor other) {

        final int host_name_comparison = host == null || other.host == null ? 0 : host.getAddress().getHostName().compareTo(other.host.getAddress().getHostName());
        return host_name_comparison != 0 ? host_name_comparison : id.compareTo(other.id);
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
