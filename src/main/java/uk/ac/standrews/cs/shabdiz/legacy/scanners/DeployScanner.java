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
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.legacy.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.legacy.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.legacy.api.ApplicationManager;

/**
 * Scanner that checks for machines that will accept an SSH connection but are not currently running the given application, i.e. that
 * are in state AUTH. For such machines an attempt is made to launch the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DeployScanner extends ConcurrentHostScanner {

    private static final String DEPLOY_SCANNER_NAME = "deploy scanner";
    private static final Logger LOGGER = Logger.getLogger(DeployScanner.class.getName());

    /** Key for the auto-deploy operation. */
    public static final String AUTO_DEPLOY_KEY = "Auto-Deploy";

    private static final Duration DEPLOY_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes a deployment scanner for the given manager.
     * 
     * @param manager the manager
     */
    public DeployScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time) {

        super(executor, manager, min_cycle_time, DEPLOY_CHECK_TIMEOUT, DEPLOY_SCANNER_NAME, false);
    }

    @Override
    public String getToggleLabel() {

        return AUTO_DEPLOY_KEY;
    }

    @Override
    protected void check(final HostDescriptor host_descriptor) {

        // If the machine is in state AUTH and auto-deploy is set, try to launch the application on the machine.
        final ApplicationManager application_manager = manager.getApplicationManager();
        if (application_manager != null && isEnabled() && isDeployable(host_descriptor)) {
            try {
                application_manager.deployApplication(host_descriptor);
            }
            catch (final Exception e) {
                LOGGER.log(Level.WARNING, "error deploying application to: " + host_descriptor.getHost(), e);
            }
        }
    }

    private boolean isDeployable(final HostDescriptor host_descriptor) {

        return host_descriptor.getHostState().equals(ApplicationState.AUTH);
    }
}