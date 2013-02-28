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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Platform;
import uk.ac.standrews.cs.shabdiz.api.State;

public class SimpleApplicationDescriptor implements ApplicationDescriptor, Comparable<SimpleApplicationDescriptor> {

    public static final String STATE_PROPERTY_NAME = "state";
    private static AtomicLong NEXT_ID = new AtomicLong();
    private final Long id; // used to resolve ties when comparing
    private final Host host;
    private final ConcurrentSkipListSet<Process> processes;
    private final AtomicReference<State> state;
    private final Pingable application_reference;
    protected final PropertyChangeSupport property_change_support;

    public SimpleApplicationDescriptor(final Host host, final Pingable application_reference) {

        id = generateId();
        this.host = new ApplicationDescriptorHostWrapper(host);
        this.application_reference = application_reference;
        processes = new ConcurrentSkipListSet<Process>();
        state = new AtomicReference<State>(State.UNKNOWN);
        property_change_support = new PropertyChangeSupport(this);
    }

    @Override
    public Host getHost() {

        return host;
    }

    @Override
    public Pingable getApplicationReference() {

        return application_reference;
    }

    @Override
    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    @Override
    public State getState() {

        return state.get();
    }

    @Override
    public void addPropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.addPropertyChangeListener(property_name, listener);
    }

    @Override
    public void removePropertyChangeListener(final String property_name, final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(property_name, listener);
    }

    /**
     * Checks if a given {@link ApplicationDescriptor} is in one of the given states.
     * 
     * @param states the states to check for
     * @return true, if the given {@link ApplicationDescriptor} is in on of the given states
     */
    public boolean isInState(final State... states) {

        final State cached_state = getState();
        for (final State state : states) {
            if (cached_state.equals(state)) { return true; }
        }
        return false;
    }

    protected void setState(final State new_state) {

        final State old_state = state.getAndSet(new_state);
        property_change_support.firePropertyChange(STATE_PROPERTY_NAME, old_state, new_state);
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
        public void shutdown() {

            host.shutdown();
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

    @Override
    public int compareTo(final SimpleApplicationDescriptor other) {

        final int host_name_comparison = host.getAddress().getHostName().compareTo(other.host.getAddress().getHostName());
        return host_name_comparison != 0 ? host_name_comparison : id.compareTo(other.id);
    }

    private static Long generateId() {

        return NEXT_ID.getAndIncrement();
    }
}
