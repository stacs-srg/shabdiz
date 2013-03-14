package uk.ac.standrews.cs.shabdiz;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.ProbeHook;

public abstract class AbstractProbeHook implements ProbeHook {

    private final AtomicReference<ApplicationState> state;
    protected final PropertyChangeSupport property_change_support;
    public static final String STATE_PROPERTY_NAME = "state";

    public AbstractProbeHook() {

        state = new AtomicReference<ApplicationState>(ApplicationState.UNKNOWN);
        property_change_support = new PropertyChangeSupport(this);
    }

    @Override
    public ApplicationState getApplicationState() {

        return state.get();
    }

    @Override
    public void setApplicationState(final ApplicationState new_state) {

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
     * Checks if a given {@link ProbeHook} is in one of the given states.
     * 
     * @param states the states to check for
     * @return true, if the given {@link ProbeHook} is in on of the given states
     */
    public boolean isInState(final ApplicationState... states) {

        final ApplicationState cached_state = getApplicationState();
        for (final ApplicationState state : states) {
            if (cached_state.equals(state)) { return true; }
        }
        return false;
    }
}
