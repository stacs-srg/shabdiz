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
package uk.ac.standrews.cs.shabdiz.scanners;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.new_api.State;

/**
 * Thread that continually checks the given list for machines that are currently running the given application, i.e. that
 * are in state RUNNING. For such machines an attempt is made to kill the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class KillScanner extends ConcurrentHostScanner {

    private static final Logger LOGGER = Logger.getLogger(KillScanner.class.getName());
    private static final String KILL_SCANNER_NAME = "kill scanner";

    /** The timeout for attempted kill checks. */
    public static final Duration DEFAULT_KILL_CHECK_TIMEOUT = new Duration(20, TimeUnit.SECONDS);

    /** Key for the auto-kill operation. */
    public static final String AUTO_KILL_KEY = "Auto-Kill";

    /**
     * Initializes a kill scanner for the given manager.
     * 
     * @param manager the manager
     */
    public KillScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time, final Duration kill_check_timeout) {

        super(executor, manager, min_cycle_time, kill_check_timeout, KILL_SCANNER_NAME, false);
    }

    @Override
    public String getToggleLabel() {

        return AUTO_KILL_KEY;
    }

    @Override
    protected void check(final HostDescriptor host_descriptor) {

        final ApplicationManager application_manager = manager.getApplicationManager();
        if (application_manager != null && isEnabled() && isKillable(host_descriptor)) {

            // If auto-kill is set, try to kill the application on the machine.
            LOGGER.info("killing application on: " + host_descriptor.getHost());
            try {
                application_manager.killApplication(host_descriptor, true);
            }
            catch (final Exception e) {
                LOGGER.log(Level.WARNING, "failed to kill application on: " + host_descriptor.getHost(), e);
            }
        }
    }

    private boolean isKillable(final HostDescriptor host_descriptor) {

        return host_descriptor.getHostState() == State.RUNNING;
    }
}
