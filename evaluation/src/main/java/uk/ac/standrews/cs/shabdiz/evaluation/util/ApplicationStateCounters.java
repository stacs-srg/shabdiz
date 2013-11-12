package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import org.mashti.gauge.Counter;
import org.mashti.gauge.MetricRegistry;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ApplicationStateCounters implements PropertyChangeListener {

    private final Iterable<ApplicationDescriptor> descriptors;
    private final Map<ApplicationState, Counter> counters;

    public ApplicationStateCounters(Iterable<ApplicationDescriptor> descriptors) {

        this.descriptors = descriptors;
        counters = new HashMap<ApplicationState, Counter>();
        initCounters();
        listenForStateChange();
    }

    public void registerTo(MetricRegistry registry) {

        for (Map.Entry<ApplicationState, Counter> entry : counters.entrySet()) {
            final String metric_name = getNameByState(entry.getKey());
            final Counter counter = entry.getValue();
            registry.register(metric_name, counter);
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent state_change_event) {

        final ApplicationState new_state = (ApplicationState) state_change_event.getNewValue();
        final ApplicationState old_state = (ApplicationState) state_change_event.getOldValue();
        counters.get(old_state).decrement();
        counters.get(new_state).increment();
    }

    private void listenForStateChange() {

        for (ApplicationDescriptor descriptor : descriptors) {
            descriptor.addStateChangeListener(this);
            final ApplicationState current_state = descriptor.getApplicationState();
            counters.get(current_state).increment();
        }
    }

    private void initCounters() {

        for (ApplicationState state : ApplicationState.values()) {
            counters.put(state, new Counter());
        }
    }

    public static String getNameByState(final ApplicationState state) {

        return state.name().toLowerCase() + "_state_counter";
    }
}
