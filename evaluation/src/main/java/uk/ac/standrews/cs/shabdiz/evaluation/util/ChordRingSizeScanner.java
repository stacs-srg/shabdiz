package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.Scanner;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.remote_management.ChordMonitoring;

public class ChordRingSizeScanner extends Scanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChordRingSizeScanner.class);
    private static final String RING_SIZE_PROPERTY_NAME = "ring_size";
    private final AtomicInteger ring_size;

    public ChordRingSizeScanner(Duration delay, Duration timeout) {

        super(delay, timeout, false);
        ring_size = new AtomicInteger();
    }

    @Override
    public synchronized void scan(final ApplicationNetwork network) {

        final IChordRemoteReference start = getFirstRunningPeer(network);
        try {

            final int ring_size_forwards = ChordMonitoring.cycleLengthFrom(start, true);
            final int ring_size_backwards = ChordMonitoring.cycleLengthFrom(start, false);

            if (isRingStable(ring_size_forwards, ring_size_backwards)) {
                final int new_ring_size = ring_size_forwards;
                final int old_ring_size = ring_size.getAndSet(new_ring_size);
                fireRingSizeChange(old_ring_size, new_ring_size);
            }
        }
        catch (final Exception e) {
            LOGGER.error("interrupted while scanning ring size", e);
        }
    }

    public Integer getLastStableRingSize() {

        return ring_size.get();
    }

    public void addRingSizeChangeListener(final PropertyChangeListener listener) {

        addPropertyChangeListener(RING_SIZE_PROPERTY_NAME, listener);
    }

    public void removeRingSizeChangeListener(final PropertyChangeListener listener) {

        removePropertyChangeListener(RING_SIZE_PROPERTY_NAME, listener);
    }

    private static boolean isRingStable(final int ring_size_forwards, final int ring_size_backwards) {

        return ring_size_forwards == ring_size_backwards;
    }

    private void fireRingSizeChange(final int old_ring_size, final int new_ring_size) {

        firePropertyChange(RING_SIZE_PROPERTY_NAME, old_ring_size, new_ring_size);
    }

    private IChordRemoteReference getFirstRunningPeer(final ApplicationNetwork network) {

        for (final ApplicationDescriptor descriptor : network) {
            if (descriptor.isInAnyOfStates(ApplicationState.RUNNING)) { return descriptor.getApplicationReference(); }
        }
        return null;
    }
}
