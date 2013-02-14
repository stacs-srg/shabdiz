/***************************************************************************
 *                                                                         *
 * remote_management Library                                               *
 * Copyright (C) 2010 Distributed Systems Architecture Research Group      *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of remote_management, a package providing             *
 * functionality for remotely managing a specified application.            *
 *                                                                         *
 * remote_management is free software: you can redistribute it and/or      *
 * modify it under the terms of the GNU General Public License as          *
 * published by the Free Software Foundation, either version 3 of the      *
 * License, or (at your option) any later version.                         *
 *                                                                         *
 * remote_management is distributed in the hope that it will be useful,    *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with remote_management.  If not, see                              *
 * <http://www.gnu.org/licenses/>.                                         *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface.scanners;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManager;
import uk.ac.standrews.cs.nds.madface.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scanner that checks for unreachable or invalid hosts, and drops them from the host list.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class DropScanner extends Scanner implements ISingleHostScanner {

    /** Key for the scanner toggle. */
    public static final String AUTO_DROP_KEY = "Auto-Drop Unreachable or Invalid Hosts";

    private static final Duration DROP_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a drop scanner for the given manager.
     * @param manager the manager
     */
    public DropScanner(final MadfaceManager manager, final int thread_pool_size, final Duration min_cycle_time) {

        super(manager, min_cycle_time, thread_pool_size, DROP_CHECK_TIMEOUT, "drop scanner", false);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getName() {

        return "Drop";
    }

    @Override
    public String getAttributeName() {

        return "Drop";
    }

    @Override
    public String getToggleLabel() {

        return AUTO_DROP_KEY;
    }

    @Override
    public void check(final HostDescriptor host_descriptor, final Set<IAttributesCallback> attribute_callbacks) {

        final HostState host_state = host_descriptor.getHostState();

        if (enabled && (host_state == HostState.UNREACHABLE || host_state == HostState.INVALID)) {

            Diagnostic.trace(DiagnosticLevel.RUN, "dropping host: " + host_descriptor.getHost());
            manager.drop(host_descriptor);
        }
    }
}
