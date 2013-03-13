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
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Platform;

public abstract class AbstractApplicationDescriptor implements ApplicationDescriptor, Comparable<AbstractApplicationDescriptor> {

    public static final String STATE_PROPERTY_NAME = "state";
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final Long id; // used to resolve ties when comparing
    private final Host host;
    private final ConcurrentSkipListSet<Process> processes;
    private final AtomicReference<ApplicationState> state;
    protected final PropertyChangeSupport property_change_support;

    public AbstractApplicationDescriptor(final Host host) {

        id = generateId();
        this.host = new ApplicationDescriptorHostWrapper(host);
        processes = new ConcurrentSkipListSet<Process>(new ProcessHashcodeComparator());
        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
        property_change_support = new PropertyChangeSupport(this);
    }

    @Override
    public Host getHost() {

        return host;
    }

    @Override
    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    @Override
    public ApplicationState getApplicationState() {

        return state.get();
    }

    @Override
    public void setState(final ApplicationState new_state) {

        final ApplicationState old_state = state.getAndSet(new_state);
        property_change_support.firePropertyChange(STATE_PROPERTY_NAME, old_state, new_state);
    }

    /**
     * Adds a {@link PropertyChangeListener} for a specific property.
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
     * Removes a {@link PropertyChangeListener} for a specific property.
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

    /**
     * Checks if a given {@link ApplicationDescriptor} is in one of the given states.
     * 
     * @param states the states to check for
     * @return true, if the given {@link ApplicationDescriptor} is in on of the given states
     */
    public boolean isInState(final ApplicationState... states) {

        final ApplicationState cached_state = getApplicationState();
        for (final ApplicationState state : states) {
            if (cached_state.equals(state)) { return true; }
        }
        return false;
    }

    @Override
    public int compareTo(final AbstractApplicationDescriptor other) {

        final int host_name_comparison = host.getAddress().getHostName().compareTo(other.host.getAddress().getHostName());
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

    private final class ApplicationDescriptorHostWrapper implements Host {

        private final Host host;

        private ApplicationDescriptorHostWrapper(final Host host) {

            this.host = host;
        }

        @Override
        public void upload(final Collection<File> sources, final String destination) throws IOException {

            host.upload(sources, destination);
        }

        @Override
        public void upload(final File source, final String destination) throws IOException {

            host.upload(source, destination);
        }

        @Override
        public void close() throws IOException {

            host.close();
        }

        @Override
        public boolean isLocal() {

            return host.isLocal();
        }

        @Override
        public Platform getPlatform() throws IOException {

            return host.getPlatform();
        }

        @Override
        public InetAddress getAddress() {

            return host.getAddress();
        }

        @Override
        public Process execute(final String... command) throws IOException {

            final Process process = host.execute(command);
            try {
                return process;
            }
            finally {
                processes.add(process);
            }
        }

        @Override
        public void download(final String source, final File destination) throws IOException {

            host.download(source, destination);
        }
    }
}
