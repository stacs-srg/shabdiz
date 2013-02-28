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
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;

/**
 * Scanner that checks for unreachable or invalid hosts, and drops them from the host list.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class AutoRemoveScanner extends AbstractConcurrentScanner<ApplicationDescriptor> {

    private static final Logger LOGGER = Logger.getLogger(AutoRemoveScanner.class.getName());

    private static final Duration DROP_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a drop scanner for the given manager.
     * 
     * @param manager the manager
     * @param application_network
     */
    public AutoRemoveScanner(final DefaultMadfaceManager manager, final Duration min_cycle_time) {

        super(  min_cycle_time, DROP_CHECK_TIMEOUT, false);
    }

    // -------------------------------------------------------------------------------------------------------

    private boolean isDroppable(final ApplicationDescriptor application_descriptor) {

        final State state = application_descriptor.getState();
        return state == State.UNREACHABLE || state == State.INVALID;
    }

    @Override
    protected void check(final ApplicationDescriptor application_descriptor) {

        if (isEnabled() && isDroppable(application_descriptor)) {
            getApplicationNetwork().remove(application_descriptor);
            LOGGER.info("removed host: " + application_descriptor.getHost());
        }
    }
}
