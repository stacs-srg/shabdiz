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
package uk.ac.standrews.cs.shabdiz.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/**
 * Given a collection of {@link ApplicationState states} listens for the change of state and {@link CountDownLatch#countDown() counts down} a latch for each state change that matches one of the given states.
 * This class is thread-safe.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LatchedStateChangeListener implements PropertyChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatchedStateChangeListener.class);
    private final ApplicationState[] states;
    private final CountDownLatch latch;

    /**
     * Instantiates a new latched state change listener.
     *
     * @param states the states to match
     */
    public LatchedStateChangeListener(final ApplicationState[] states) {

        this.states = states.clone();
        latch = new CountDownLatch(1);
    }

    @Override
    public synchronized void propertyChange(final PropertyChangeEvent evt) {

        LOGGER.trace("state changed to {} on {}", evt.getNewValue(), evt.getSource());

        final ApplicationState new_state = (ApplicationState) evt.getNewValue();
        if (ArrayUtil.contains(new_state, states)) {
            latch.countDown();
            LOGGER.trace("counted down latch for the matching state {}", new_state);
        }
    }

    /**
     * Awaits until a state change event is fired, which its new value matches one of this listeners states.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void await() throws InterruptedException {

        latch.await();
    }
}
