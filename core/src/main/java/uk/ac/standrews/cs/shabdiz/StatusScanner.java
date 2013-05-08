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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Scanner that monitors machine status. Machines are probed for the presence of a particular application, and for their willingness to accept an SSH connection with specified credentials.
 * The results of these tests are recorded in the corresponding host descriptors.
 * This scanner publishes a new latch after every cycle through the host list. This enables other scanners to synchronize their own operation with this one.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class StatusScanner extends AbstractConcurrentScanner {

    /** The default timeout for attempted status checks. */
    public static final Duration DEFAULT_STATUS_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final boolean ENABLED_BY_DEFAULT = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusScanner.class);

    protected StatusScanner(final Duration cycle_delay) {

        this(cycle_delay, DEFAULT_STATUS_CHECK_TIMEOUT, ENABLED_BY_DEFAULT);
    }

    protected StatusScanner(final Duration cycle_delay, final Duration status_check_timeout, final boolean enabled) {

        super(cycle_delay, status_check_timeout, enabled);
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        if (isEnabled()) {
            final ApplicationState new_state = descriptor.getApplicationManager().probeApplicationState(descriptor);
            LOGGER.debug("new state {}", new_state);
            descriptor.setCachedApplicationState(new_state);
        }
        else {
            LOGGER.debug("status scan is disabled");
        }
    }
}
