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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Thread that continually checks the given list for machines that are currently running the given application, i.e. that
 * are in state RUNNING. For such machines an attempt is made to kill the application.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoKillScanner extends AbstractConcurrentScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoKillScanner.class);

    protected AutoKillScanner(final Duration min_cycle_time, final Duration kill_check_timeout) {

        super(min_cycle_time, kill_check_timeout, false);
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        if (isEnabled() && isKillable(descriptor)) {
            try {
                descriptor.getApplicationManager().kill(descriptor);
            } catch (final Exception e) {
                LOGGER.warn("failed to terminate descriptor", e);
            }
        }
    }

    private boolean isKillable(final ApplicationDescriptor application_descriptor) {

        return application_descriptor.getApplicationState() == ApplicationState.RUNNING;
    }
}
