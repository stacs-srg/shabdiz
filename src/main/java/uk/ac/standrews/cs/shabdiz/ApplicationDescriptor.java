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
import uk.ac.standrews.cs.shabdiz.util.SelfRemovingStateChangeListener;

/**
 * Describes an application instance that is managed by an {@link ApplicationNetwork application network}.
 * Instances of this class are in one-to-one correspondence to the application instances within an {@link ApplicationNetwork application network}.
 * However, a {@link Host} and/or an {@link ApplicationManager} may be associated to multiple {@link ApplicationDescriptor application descriptors}.
 * The {@link #getHost() host} of an application descriptor may be {@code null}.
 * However, the {@link #getApplicationManager() manager} must not be {@code null}.
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

    /**
     * Instantiates a new application descriptor with the given {@link ApplicationManager manager} and {@code null} as {@link #getHost() host}.
     * The initial {@link #getCachedApplicationState() state} is set to {@link ApplicationState#UNKNOWN}.
     * 
     * @param application_manager the manager of the application instance
     */
    public ApplicationDescriptor(final ApplicationManager application_manager) {

        this(null, application_manager);
    }

    /**
     * Instantiates a new application descriptor with the given {@link Host host} and {@link ApplicationManager manager}.
     * The initial {@link #getCachedApplicationState() state} is set to {@link ApplicationState#UNKNOWN}.
     * 
     * @param host the host of the application instance that is described by this descriptor
     * @param application_manager the manager of the application instance
     */
    public ApplicationDescriptor(final Host host, final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        this.host = createHostWrapper(host);
        id = generateId();
        processes = new ConcurrentSkipListSet<Process>(new ProcessHashcodeComparator());
        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
        application_reference = new AtomicReference<Object>();
        property_change_support = new PropertyChangeSupport(this);
    }

    /**
     * Gets the manager of application instance that is described by this descriptor.
     * 
     * @return the manager of application instance that is described by this descriptor
     */
    public ApplicationManager getApplicationManager() {

        return application_manager;
    }

    /**
     * Gets the host of the application instance that is associated to this descriptor, or {@code null} if no host is associated to this descriptor.
     * 
     * @return the host of the application instance that is associated to this descriptor, or {@code null} if no host is associated to this descriptor
     */
    public Host getHost() {

        return host;
    }

    /**
     * Gets the set of processes that are started using this descrptor's {@link #getHost() host}.
     * The collection of processes are updated automatically when a command is {@link Host#execute(String...) executed} by this descriptor's {@link #getHost() host}.
     * 
     * @return the set of processes that are {@link Host#execute(String...) started} using this descrptor's {@link #getHost() host}
     */
    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    /**
     * Gets the cached state of the application instance that is described by this descriptor.
     * 
     * @return the cached state of the application instance that is described by this descriptor
     */
    public ApplicationState getCachedApplicationState() {

        return state.get();
    }

    /**
     * Gets a reference to the application instance that is {@link ApplicationManager#deploy(ApplicationDescriptor) deployed} by the {@link #getApplicationManager() manager} of this descriptor.
     * The application reference is casted to the type of the variable, which stores the returned value of this method.
     * This may cause {@link ClassCastException} if the application reference cannot be cased to the variable type.
     * This method may return {@code null} if no instance was {@link ApplicationManager#deploy(ApplicationDescriptor) deployed} by this descriptor's {@link #getApplicationManager() manager}.
     * 
     * @param <ApplicationReference> the type of application reference to which the application reference is casted
     * @return a reference to the application instance that is described by this descriptor, or {@code null} if no instance was deployed by this descriptor's {@link #getApplicationManager() manager}
     */
    @SuppressWarnings("unchecked")
    public <ApplicationReference> ApplicationReference getApplicationReference() {

        return (ApplicationReference) application_reference.get();
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

    @SuppressWarnings("resource")
    private ApplicationDescriptorHostWrapper createHostWrapper(final Host host) {

        return host != null ? new ApplicationDescriptorHostWrapper(host) : null;
    }

    private final class ApplicationDescriptorHostWrapper extends HostWrapper {

        private ApplicationDescriptorHostWrapper(final Host host) {

            super(host);
        }

        @Override
        public Process execute(final String... command) throws IOException {

            final Process process = getUnwrappedHost().execute(command);
            addProcess(process);
            return process;
        }
    }

    private static final class ProcessHashcodeComparator implements Comparator<Process> {

        @Override
        public int compare(final Process o1, final Process o2) {

            return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
        }
    }
}
