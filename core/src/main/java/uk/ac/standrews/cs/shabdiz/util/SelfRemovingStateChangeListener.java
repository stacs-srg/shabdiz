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

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/**
 * Given an {@link ApplicationDescriptor}, a {@link CountDownLatch} and a collection of {@link ApplicationState states} listens for the change of state in the given application descriptor.
 * If the new state matches one of the given {@code states}, {@link CountDownLatch#countDown() counts down} the given {@code latch}, and finally removes itself from the application descriptor's property change listeners.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class SelfRemovingStateChangeListener implements PropertyChangeListener {

    private final ApplicationDescriptor application_descriptor;
    private final ApplicationState[] states;
    private final CountDownLatch latch;

    /**
     * Instantiates a new self removing state change listener.
     * 
     * @param application_descriptor the application_descriptor
     * @param states the states
     * @param latch the latch
     */
    public SelfRemovingStateChangeListener(final ApplicationDescriptor application_descriptor, final ApplicationState[] states, final CountDownLatch latch) {

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
