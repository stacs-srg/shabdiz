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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.State;

/**
 * Scanner that monitors machine status. Machines are probed for the presence of a particular application, and for their willingness to accept an SSH connection with specified credentials.
 * The results of these tests are recorded in the corresponding host descriptors.
 * This scanner publishes a new latch after every cycle through the host list. This enables other scanners to synchronize their own operation with this one.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class StatusScanner extends ConcurrentHostScanner {

    private static final String STATUS_SCANNER_NAME = "status scanner";

    /** The default timeout for attempted SSH connections. This value determined by experiment. */
    public static final Duration DEFAULT_SSH_CHECK_TIMEOUT = new Duration(15, TimeUnit.SECONDS);

    /** The default timeout for attempted status checks. */
    public static final Duration DEFAULT_STATUS_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    /** Key for the host scanning operation. */
    public static final String STATUS_SCANNER_KEY = "Status Scanner";

    // A minimal shell command that will be attempted in order to check ssh connectivity. Chosen to have minimal dependency on execution environment, so doesn't rely on anything specific to a user.
    private static final String MINIMAL_COMMAND = "cd /";

    private static final boolean ENABLED_BY_DEFAULT = false;

    private volatile CountDownLatch cycle_latch;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a status scanner for the given manager.
     * 
     * @param manager the manager
     * @param min_cycle_time the minimum time between successive cycles
     * @param status_check_timeout the timeout for attempted status checks
     * @param ssh_check_timeout the timeout for attempted SSH connections
     */
    public StatusScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time, final Duration status_check_timeout, final Duration ssh_check_timeout) {

        super(executor, manager, min_cycle_time, status_check_timeout, STATUS_SCANNER_NAME, ENABLED_BY_DEFAULT);

        // Initialize the latch for the first cycle.
        newCycleLatch();
    }

    @Override
    public String getToggleLabel() {

        return STATUS_SCANNER_KEY;
    }

    @Override
    protected void check(final HostDescriptor host_descriptor) {

        final ApplicationManager application_manager = manager.getApplicationManager();

        if (application_manager != null && isEnabled()) {

            // First try to contact the application directly. If that doesn't work, try to make an SSH connection to
            // see whether it would be possible to launch the application on the machine.

            try {
                application_manager.attemptApplicationCall(host_descriptor);
                setHostState(host_descriptor, State.RUNNING);
            }
            catch (final UnknownHostException e) {
                // Machine address couldn't be resolved.
                setHostState(host_descriptor, State.INVALID);
            }
            catch (final Exception e) {
                e.printStackTrace();
                try {
                    // Application call failed, so try SSH connection.
                    attemptSSHConnection(host_descriptor);
                    setHostState(host_descriptor, State.AUTH);
                }
                catch (final SSHException e1) {

                    // Couldn't make SSH connection with specified credentials.
                    setHostState(host_descriptor, State.NO_AUTH);
                }
                catch (final UnknownHostException e1) {

                    // Machine address couldn't be resolved.
                    setHostState(host_descriptor, State.INVALID);
                }
                catch (final IOException e1) {

                    // Network error trying to make SSH connection.
                    setHostState(host_descriptor, State.UNREACHABLE);
                }
                catch (final TimeoutException e1) {

                    // SSH connection timed out.
                    setHostState(host_descriptor, State.UNREACHABLE);
                }
                catch (final InterruptedException e1) {

                    // SSH connection timed out.
                    setHostState(host_descriptor, State.UNREACHABLE);
                }
            }
        }
    }

    @Override
    public CountDownLatch getCycleLatch() {

        return cycle_latch;
    }

    @Override
    public void cycleFinished() {

        // Release all scanner threads waiting on the current latch, then create a new latch for the next cycle.
        newCycleLatch();
    }

    // -------------------------------------------------------------------------------------------------------

    private void newCycleLatch() {

        if (cycle_latch != null) {
            cycle_latch.countDown();
        }
        cycle_latch = new CountDownLatch(1);
    }

    private void attemptSSHConnection(final HostDescriptor host_descriptor) throws IOException, TimeoutException, InterruptedException {

        // Try to execute a 'cd /' shell command on the machine.
        // This is selected as a command that produces no output and doesn't require a functioning home directory.
        //FIXME add timeout
        final Process ssh_test_process = host_descriptor.getManagedHost().execute(MINIMAL_COMMAND);
        ssh_test_process.waitFor();
        ssh_test_process.destroy();
    }

    private void setHostState(final HostDescriptor host_descriptor, final State new_state) {

        host_descriptor.hostState(new_state);
    }
}
