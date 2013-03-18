package uk.ac.standrews.cs.shabdiz;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CountDownLatch;

final class SelfRemovingStateChangeListener implements PropertyChangeListener {

    private final ApplicationDescriptor application_descriptor;
    private final ApplicationState[] states;
    private final CountDownLatch latch;

    SelfRemovingStateChangeListener(final ApplicationDescriptor application_descriptor, final ApplicationState[] states, final CountDownLatch latch) {

        this.application_descriptor = application_descriptor;
        this.states = states;
        this.latch = latch;
    }

    @Override
    public synchronized void propertyChange(final PropertyChangeEvent evt) {

        if (application_descriptor.isInState(states)) {
            latch.countDown();
            application_descriptor.removeStateChangeListener(this);
        }
    }
}