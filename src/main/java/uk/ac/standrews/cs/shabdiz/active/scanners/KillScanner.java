/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active.scanners;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.HostState;
import uk.ac.standrews.cs.shabdiz.active.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.AttributesCallback;
import uk.ac.standrews.cs.shabdiz.active.interfaces.SingleHostScanner;

/**
 * Thread that continually checks the given list for machines that are currently running the given application, i.e. that
 * are in state RUNNING. For such machines an attempt is made to kill the application.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class KillScanner extends Scanner implements SingleHostScanner {

    /** The timeout for attempted kill checks. */
    public static final Duration DEFAULT_KILL_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    /**
     * Key for the auto-kill operation.
     */
    public static final String AUTO_KILL_KEY = "Auto-Kill";

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a kill scanner for the given manager.
     * @param manager the manager
     */
    public KillScanner(final MadfaceManager manager, final int thread_pool_size, final Duration min_cycle_time, final Duration kill_check_timeout) {

        super(manager, min_cycle_time, thread_pool_size, kill_check_timeout, "kill scanner", false);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getName() {

        return "Kill";
    }

    @Override
    public String getAttributeName() {

        return "Kill";
    }

    @Override
    public String getToggleLabel() {

        return AUTO_KILL_KEY;
    }

    @Override
    public void check(final HostDescriptor host_descriptor, final Set<AttributesCallback> attribute_callbacks) throws Exception {

        final ApplicationManager application_manager = manager.getApplicationManager();

        if (application_manager != null && enabled) {

            // If auto-kill is set, try to kill the application on the machine.
            if (host_descriptor.getHostState() == HostState.RUNNING) {

                Diagnostic.trace(DiagnosticLevel.FULL, "killing application on: " + host_descriptor.getHost());

                application_manager.killApplication(host_descriptor, false);
            }
        }
    }
}
