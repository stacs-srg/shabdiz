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
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Scanner that checks for unreachable or invalid hosts, and drops them from the host list.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoRemoveScanner extends AbstractConcurrentScanner {

    private static final Duration DROP_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    protected AutoRemoveScanner(final Duration cycle_delay) {

        super(cycle_delay, DROP_CHECK_TIMEOUT, false);
    }

    protected boolean isRemovable(final ApplicationDescriptor application_descriptor) {

        final ApplicationState state = application_descriptor.getApplicationState();
        return state == ApplicationState.UNREACHABLE || state == ApplicationState.INVALID;
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        if (isRemovable(descriptor)) {
            network.remove(descriptor);
        }
    }
}
