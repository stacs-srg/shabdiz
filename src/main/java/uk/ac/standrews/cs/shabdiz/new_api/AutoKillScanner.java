/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.new_api;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Thread that continually checks the given list for machines that are currently running the given application, i.e. that
 * are in state RUNNING. For such machines an attempt is made to kill the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoKillScanner extends AbstractConcurrentScanner<ApplicationDescriptor> {

    private static final Logger LOGGER = Logger.getLogger(AutoKillScanner.class.getName());

    /** The timeout for attempted kill checks. */
    public static final Duration DEFAULT_KILL_CHECK_TIMEOUT = new Duration(20, TimeUnit.SECONDS);

    protected AutoKillScanner(final Duration min_cycle_time, final Duration kill_check_timeout) {

        super(min_cycle_time, DEFAULT_KILL_CHECK_TIMEOUT, false);
    }

    private boolean isKillable(final ApplicationDescriptor application_descriptor) {

        return application_descriptor.getState() == State.RUNNING;
    }

    @Override
    protected void check(final ApplicationDescriptor application_descriptor) {

        if (isEnabled() && isKillable(application_descriptor)) {

            // If auto-kill is set, try to kill the application on the machine.
            LOGGER.info("killing application on: " + application_descriptor.getHost());
            try {
                getApplicationNetwork().kill(application_descriptor);
            }
            catch (final Exception e) {
                LOGGER.log(Level.WARNING, "failed to kill application on: " + application_descriptor.getHost(), e);
            }
        }

    }
}
