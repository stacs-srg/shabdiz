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
package uk.ac.standrews.cs.nds.madface.scanners;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManager;
import uk.ac.standrews.cs.nds.madface.interfaces.IApplicationManager;
import uk.ac.standrews.cs.nds.madface.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scanner that checks for machines that will accept an SSH connection but are not currently running the given application, i.e. that
 * are in state AUTH. For such machines an attempt is made to launch the application.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class DeployScanner extends Scanner implements ISingleHostScanner {

    /** Key for the auto-deploy operation. */
    public static final String AUTO_DEPLOY_KEY = "Auto-Deploy";

    /** The default thread pool size for deploy scan checks. This value determined by experiment. */
    public static final int DEFAULT_SCANNER_THREAD_POOL_SIZE = 10;

    private static final Duration DEPLOY_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a deployment scanner for the given manager.
     * @param manager the manager
     */
    public DeployScanner(final MadfaceManager manager, final int thread_pool_size, final Duration min_cycle_time) {

        super(manager, min_cycle_time, thread_pool_size, DEPLOY_CHECK_TIMEOUT, "deploy scanner", false);
    }

    @Override
    public String getName() {

        return "Deploy";
    }

    @Override
    public String getAttributeName() {

        return "Deploy";
    }

    @Override
    public String getToggleLabel() {

        return AUTO_DEPLOY_KEY;
    }

    @Override
    public void check(final HostDescriptor host_descriptor, final Set<IAttributesCallback> attribute_callbacks) {

        final IApplicationManager application_manager = manager.getApplicationManager();

        if (application_manager != null) {

            // If the machine is in state AUTH and auto-deploy is set, try to launch the application on the machine.
            if (host_descriptor.getHostState() == HostState.AUTH && enabled) {

                try {
                    //FIXME think of something for setDiscardErrors
                    // host_descriptor.getProcessManager().setDiscardErrors(manager.errorsAreDiscarded());
                    application_manager.deployApplication(host_descriptor);
                }
                catch (final Exception e) {
                    Diagnostic.trace(DiagnosticLevel.FULL, "error deploying application to: " + host_descriptor.getHost() + " - " + e.getMessage());
                }
            }
        }
    }
}
