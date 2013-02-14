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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManager;
import uk.ac.standrews.cs.nds.madface.ProcessDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.nds.madface.interfaces.IApplicationManager;
import uk.ac.standrews.cs.nds.madface.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.IHostStatusCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Scanner that monitors machine status. Machines are probed for the presence of a particular application, and for their willingness to accept an SSH connection with specified credentials.
 * The results of these tests are recorded in the corresponding host descriptors.
 * 
 * This scanner publishes a new latch after every cycle through the host list. This enables other scanners to synchronize their own operation with this one.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class StatusScanner extends Scanner implements ISingleHostScanner {

    /** The default thread pool size for status scan checks. This value determined by experiment. */
    public static final int DEFAULT_SCANNER_THREAD_POOL_SIZE = 5;

    /** The default thread pool size for SSH access checks. This value determined by experiment. */
    public static final int DEFAULT_SSH_CHECK_THREAD_POOL_SIZE = 5;

    /** The default timeout for attempted SSH connections. This value determined by experiment. */
    public static final Duration DEFAULT_SSH_CHECK_TIMEOUT = new Duration(15, TimeUnit.SECONDS);

    /** The default timeout for attempted status checks. */
    public static final Duration DEFAULT_STATUS_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    /** Key for the host scanning operation. */
    public static final String STATUS_SCANNER_KEY = "Status Scanner";

    // A minimal shell command that will be attempted in order to check ssh connectivity. Chosen to have minimal dependency on execution environment, so doesn't rely on anything specific to a user.
    private static final String MINIMAL_COMMAND = "cd /";

    private static final boolean ENABLED_BY_DEFAULT = false;

    private final Set<IHostStatusCallback> host_status_callbacks;
    private final ProcessDescriptor ssh_connection_process_descriptor;
    private final TimeoutExecutor ssh_timeout_executor;

    private volatile CountDownLatch cycle_latch;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a status scanner for the given manager.
     *
     * @param manager the manager
     * @param min_cycle_time the minimum time between successive cycles
     * @param thread_pool_size the thread pool size for status scan checks
     * @param ssh_check_thread_pool_size the thread pool size for SSH access checks
     * @param status_check_timeout the timeout for attempted status checks
     * @param ssh_check_timeout the timeout for attempted SSH connections
     */
    public StatusScanner(final MadfaceManager manager, final Duration min_cycle_time, final int thread_pool_size, final int ssh_check_thread_pool_size, final Duration status_check_timeout, final Duration ssh_check_timeout) {

        super(manager, min_cycle_time, thread_pool_size, status_check_timeout, "status scanner", ENABLED_BY_DEFAULT);

        host_status_callbacks = manager.getHostStatusCallbacks();
        ssh_timeout_executor = TimeoutExecutor.makeTimeoutExecutor(ssh_check_thread_pool_size, ssh_check_timeout, true, false, "StatusScanner timeout executor");
        ssh_connection_process_descriptor = new ProcessDescriptor().command(MINIMAL_COMMAND).executor(ssh_timeout_executor);

        // Initialize the latch for the first cycle.
        newCycleLatch();
    }

    @Override
    public void shutdown() {

        super.shutdown();
        ssh_timeout_executor.shutdown();
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getName() {

        return "Status";
    }

    @Override
    public String getAttributeName() {

        return "Status";
    }

    @Override
    public String getToggleLabel() {

        return STATUS_SCANNER_KEY;
    }

    @Override
    public void check(final HostDescriptor host_descriptor, final Set<IAttributesCallback> attribute_callbacks) {

        final IApplicationManager application_manager = manager.getApplicationManager();

        if (application_manager != null && enabled) {

            final HostState original_state = host_descriptor.getHostState();

            // First try to contact the application directly. If that doesn't work, try to make an SSH connection to
            // see whether it would be possible to launch the application on the machine.

            try {
                application_manager.attemptApplicationCall(host_descriptor);
                setHostState(host_descriptor, HostState.RUNNING);
            }
            catch (final UnknownHostException e) {
                // Machine address couldn't be resolved.
                setHostState(host_descriptor, HostState.INVALID);
            }
            catch (final Exception e) {
                try {

                    // Application call failed, so try SSH connection.
                    attemptSSHConnection(host_descriptor);

                    setHostState(host_descriptor, HostState.AUTH);
                }
                catch (final SSH2Exception e1) {

                    // Couldn't make SSH connection with specified credentials.
                    setHostState(host_descriptor, HostState.NO_AUTH);
                }
                catch (final UnsupportedPlatformException e1) {

                    // SSH connections not supported by remote platform.
                    setHostState(host_descriptor, HostState.NO_AUTH);
                }
                catch (final UnknownHostException e1) {

                    // Machine address couldn't be resolved.
                    setHostState(host_descriptor, HostState.INVALID);
                }
                catch (final IOException e1) {

                    // Network error trying to make SSH connection.
                    setHostState(host_descriptor, HostState.UNREACHABLE);
                }
                catch (final TimeoutException e1) {

                    // SSH connection timed out.
                    setHostState(host_descriptor, HostState.UNREACHABLE);
                }
                catch (final InterruptedException e1) {

                    // SSH connection timed out.
                    setHostState(host_descriptor, HostState.UNREACHABLE);
                }
                catch (final UnknownPlatformException e1) {

                    // This shouldn't happen...
                    Diagnostic.trace("Unexpected unknown platform exception");
                    setHostState(host_descriptor, HostState.UNKNOWN);
                }
            }

            final HostState new_state = host_descriptor.getHostState();
            Diagnostic.trace(DiagnosticLevel.FULL, "state: " + new_state);

            if (new_state != original_state && host_status_callbacks.size() > 0) {
                for (final IHostStatusCallback callback : host_status_callbacks) {
                    callback.hostStatusChange(host_descriptor, original_state);
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

    private void attemptSSHConnection(final HostDescriptor host_descriptor) throws SSH2Exception, IOException, TimeoutException, UnknownPlatformException, InterruptedException, UnsupportedPlatformException {

        // Try to execute a 'cd /' shell command on the machine.
        // This is selected as a command that produces no output and doesn't require a functioning home directory.

        host_descriptor.getProcessManager().runProcess(ssh_connection_process_descriptor);
    }

    private void setHostState(final HostDescriptor host_descriptor, final HostState new_state) {

        final HostState old_state = host_descriptor.getHostState();
        host_descriptor.hostState(new_state);

        if (new_state != old_state) {
            Diagnostic.traceNoSource(DiagnosticLevel.RUN, host_descriptor.getHost() + ": " + new_state);
        }
    }
}
