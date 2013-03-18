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
package uk.ac.standrews.cs.shabdiz.legacy.scanners;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.legacy.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.legacy.HostDescriptor;

/**
 * Scanner that checks for unreachable or invalid hosts, and drops them from the host list.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class DropScanner extends ConcurrentHostScanner {

    private static final String DROP_SCANNER_NAME = "drop scanner";

    private static final Logger LOGGER = Logger.getLogger(DropScanner.class.getName());

    /** Key for the scanner toggle. */
    public static final String AUTO_DROP_KEY = "Auto-Drop Unreachable or Invalid Hosts";

    private static final Duration DROP_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a drop scanner for the given manager.
     * 
     * @param manager the manager
     */
    public DropScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time) {

        super(executor, manager, min_cycle_time, DROP_CHECK_TIMEOUT, DROP_SCANNER_NAME, false);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getToggleLabel() {

        return AUTO_DROP_KEY;
    }

    @Override
    protected void check(final HostDescriptor host_descriptor) {

        if (isEnabled() && isDroppable(host_descriptor)) {
            LOGGER.info("dropping host: " + host_descriptor.getHost());
            manager.drop(host_descriptor);
        }
    }

    private boolean isDroppable(final HostDescriptor host_descriptor) {

        final ApplicationState host_state = host_descriptor.getHostState();
        return host_state == ApplicationState.UNREACHABLE || host_state == ApplicationState.INVALID;
    }
}
