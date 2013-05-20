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
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.ArrayUtil;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.HostWrapper;
import uk.ac.standrews.cs.shabdiz.util.LatchedStateChangeListener;

/**
 * Presents an application instance that is running on a {@link Host host}.
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
    protected final PropertyChangeSupport property_change_support;
    private final Long id; // used to define ordering on descriptors
    private final Host host;
    private final AtomicReference<ApplicationState> state;
    private final AtomicReference<Object> application_reference;
    private final Set<Process> processes;
    private final ApplicationManager application_manager;
    private final ConcurrentHashMap<AttributeKey<?>, Object> attributes;

    /**
     * Instantiates a new application descriptor with the given {@link ApplicationManager manager} and {@code null} as {@link #getHost() host}.
     * The initial {@link #getApplicationState() state} is set to {@link ApplicationState#UNKNOWN}.
     * 
     * @param application_manager the manager of the application instance
     */
    public ApplicationDescriptor(final ApplicationManager application_manager) {

        this(null, application_manager);
    }

    /**
     * Instantiates a new application descriptor with the given {@link Host host} and {@link ApplicationManager manager}.
     * The initial {@link #getApplicationState() state} is set to {@link ApplicationState#UNKNOWN}.
     * 
     * @param host the host of the application instance that is described by this descriptor
     * @param application_manager the manager of the application instance
     */
    public ApplicationDescriptor(final Host host, final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        this.host = createHostWrapper(host);
        id = generateId();
        processes = Collections.synchronizedSet(new HashSet<Process>());
        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
        application_reference = new AtomicReference<Object>();
        property_change_support = new PropertyChangeSupport(this);
        attributes = new ConcurrentHashMap<AttributeKey<?>, Object>();
    }

    private static Long generateId() {

        return NEXT_ID.getAndIncrement();
    }

    @SuppressWarnings("resource")
    private ApplicationDescriptorHostWrapper createHostWrapper(final Host host) {

        return host != null ? new ApplicationDescriptorHostWrapper(host) : null;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * 
     * @param <Value> the type of the value
     * @param key the key whose associated value is to be returned
     * @return Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * @see ConcurrentSkipListMap#get(Object)
     */
    @SuppressWarnings("unchecked")
    public <Value> Value getAttribute(final AttributeKey<Value> key) {

        return (Value) attributes.get(key);
    }

    /**
     * Associates the specified value with the specified key in this descriptor's attributes.
     * If the attributes previously contained a mapping for the key, the old value is replaced.
     * 
     * @param <Value> the type of the value
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping for the key
     * @see ConcurrentSkipListMap#put(Object, Object)
     */
    @SuppressWarnings("unchecked")
    public <Value> Value setAttribute(final AttributeKey<Value> key, final Value value) {

        return value == null ? (Value) attributes.remove(key) : (Value) attributes.put(key, value);
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
     * The collection of processes are updated automatically when a command is {@link Host#execute(String) executed} by this descriptor's {@link #getHost() host}.
     * 
     * @return the set of processes that are {@link Host#execute(String) started} using this descrptor's {@link #getHost() host}
     */
    public Set<Process> getProcesses() {

        return new CopyOnWriteArraySet<Process>(processes);
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    protected boolean removeProcess(final Process process) {

        return processes.remove(process);
    }

    protected void clearProcesses() {

        processes.clear();
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

    /**
     * Causes the current thread to wait until this descriptor reaches one of the given {@code states}, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states which this application instance must reach
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        LatchedStateChangeListener latched_listener = null;
        synchronized (this) { // lock on this to prevent the state from changing between checking the current state and adding the listener
            if (!isInAnyOfStates(states)) {
                latched_listener = new LatchedStateChangeListener(states);
                addStateChangeListener(latched_listener);
            }
        }

        if (latched_listener != null) {
            try {
                latched_listener.await();
            }
            finally {
                removeStateChangeListener(latched_listener);
            }
        }
    }

    /**
     * Adds a {@link PropertyChangeListener} for the {@link #getApplicationState() cached state} property.
     * If listener is {@code null} no exception is thrown and no action is taken.
     * 
     * @param listener the listener to be added
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void addStateChangeListener(final PropertyChangeListener listener) {

        property_change_support.addPropertyChangeListener(STATE_PROPERTY_NAME, listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} for the {@link #getApplicationState() cached state} property.
     * If listener is {@code null} or was never added for the specified property, no exception is thrown and no action is taken.
     * 
     * @param listener the listener to be removed
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void removeStateChangeListener(final PropertyChangeListener listener) {

        property_change_support.removePropertyChangeListener(STATE_PROPERTY_NAME, listener);
    }

    /**
     * Checks if the {@link ApplicationDescriptor#getApplicationState() state} of this descriptor is equal to one of the given {@code states}.
     * 
     * @param states the states to check for
     * @return {@code true} if the {@link ApplicationDescriptor#getApplicationState() state} of this descriptor is equal to one of the given {@code states}, {@code false} otherwise
     */
    public boolean isInAnyOfStates(final ApplicationState... states) {

        return ArrayUtil.contains(getApplicationState(), states);
    }

    /**
     * Gets the cached state of the application instance that is described by this descriptor.
     * 
     * @return the cached state of the application instance that is described by this descriptor
     */
    public synchronized ApplicationState getApplicationState() {

        return state.get();
    }

    protected synchronized void setApplicationState(final ApplicationState new_state) {

        final ApplicationState old_state = state.getAndSet(new_state);
        property_change_support.firePropertyChange(STATE_PROPERTY_NAME, old_state, new_state);
    }

    @Override
    public int compareTo(final ApplicationDescriptor other) {

        return id.compareTo(other.id);
    }

    @Override
    public int hashCode() {

        return id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof ApplicationDescriptor)) { return false; }

        final ApplicationDescriptor that = (ApplicationDescriptor) other;
        return id.equals(that.id);
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder("ApplicationDescriptor{");
        sb.append("id=").append(id);
        sb.append(", host=").append(host);
        sb.append(", state=").append(state);
        sb.append(", application_reference=").append(application_reference);
        sb.append(", application_manager=").append(application_manager);
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    private final class ApplicationDescriptorHostWrapper extends HostWrapper {

        private ApplicationDescriptorHostWrapper(final Host host) {

            super(host);
        }

        @Override
        public Process execute(final String command) throws IOException {

            final Process process = getUnwrappedHost().execute(command);
            addProcess(process);
            return process;
        }

        @Override
        public Process execute(final String working_directory, final String command) throws IOException {

            final Process process = getUnwrappedHost().execute(working_directory, command);
            addProcess(process);
            return process;
        }
    }
}
