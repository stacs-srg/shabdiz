/*
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

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.api.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;

/**
 * Thread that continually checks the given list for machines that are currently running the given application, i.e. that
 * are in state RUNNING. For such machines an attempt is made to kill the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoKillScanner extends AbstractConcurrentScanner {

    /** The timeout for attempted kill checks. */
    public static final Duration DEFAULT_KILL_CHECK_TIMEOUT = new Duration(20, TimeUnit.SECONDS);

    protected AutoKillScanner(final Duration min_cycle_time, final Duration kill_check_timeout) {

        super(min_cycle_time, DEFAULT_KILL_CHECK_TIMEOUT, false);
    }

    private boolean isKillable(final ApplicationDescriptor application_descriptor) {

        return application_descriptor.getCachedApplicationState() == ApplicationState.RUNNING;
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        if (isEnabled() && isKillable(descriptor)) {
            try {
                descriptor.getApplicationManager().kill(descriptor);
            }
            catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
