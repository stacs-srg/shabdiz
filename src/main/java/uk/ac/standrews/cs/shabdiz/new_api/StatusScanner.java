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
package uk.ac.standrews.cs.shabdiz.new_api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;
import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scanner that monitors machine status. Machines are probed for the presence of a particular application, and for their willingness to accept an SSH connection with specified credentials.
 * The results of these tests are recorded in the corresponding host descriptors.
 * This scanner publishes a new latch after every cycle through the host list. This enables other scanners to synchronize their own operation with this one.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class StatusScanner<T extends SimpleApplicationDescriptor> extends AbstractConcurrentScanner<T> {

    /** The default timeout for attempted SSH connections. This value determined by experiment. */
    public static final Duration DEFAULT_SSH_CHECK_TIMEOUT = new Duration(15, TimeUnit.SECONDS);

    /** The default timeout for attempted status checks. */
    public static final Duration DEFAULT_STATUS_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    // A minimal shell command that will be attempted in order to check ssh connectivity. Chosen to have minimal dependency on execution environment, so doesn't rely on anything specific to a user.
    private static final String MINIMAL_COMMAND = "cd /"; //TODO is this platform dependent?

    private static final boolean ENABLED_BY_DEFAULT = false;

    private final Duration ssh_check_timeout;

    protected StatusScanner(final Duration cycle_delay) {

        this(cycle_delay, DEFAULT_STATUS_CHECK_TIMEOUT, DEFAULT_SSH_CHECK_TIMEOUT, ENABLED_BY_DEFAULT);
    }

    protected StatusScanner(final Duration cycle_delay, final Duration status_check_timeout, final Duration ssh_check_timeout, final boolean enabled) {

        super(cycle_delay, status_check_timeout, enabled);
        this.ssh_check_timeout = ssh_check_timeout;
    }

    @Override
    protected void check(final T application_descriptor) {

        if (isEnabled()) {
            State state;
            try {
                final Pingable application_reference = application_descriptor.getApplicationReference();
                state = getStateFromApplicationReference(application_reference);
            }
            catch (final RPCException e) {
                final Host host = application_descriptor.getHost();
                state = getStateFromHost(host);
            }
            application_descriptor.setState(state);
        }
    }

    private State getStateFromHost(final Host host) {

        //TODO tidy up
        try {
            attemptAddressResolution(host.getAddress().getHostName());
            // Application call failed, so try SSH connection.
            attemptCommandExecution(host);
            return State.AUTH;
        }
        catch (final UnknownHostException e) {

            // Machine address couldn't be resolved.
            return State.INVALID;
        }
        catch (final SSHException e) {

            // Couldn't make SSH connection with specified credentials.
            return State.NO_AUTH;
        }
        catch (final IOException e) {

            // Network error trying to make SSH connection.
            return State.UNREACHABLE;
        }
        catch (final TimeoutException e) {

            // SSH connection timed out.
            return State.UNREACHABLE;
        }
        catch (final InterruptedException e) {

            // SSH connection timed out.
            return State.UNREACHABLE;
        }
    }

    private void attemptAddressResolution(final String host_name) throws UnknownHostException {

        InetAddress.getByName(host_name);
    }

    private State getStateFromApplicationReference(final Pingable application_reference) throws RPCException {

        application_reference.ping();
        return State.RUNNING;
    }

    private void attemptCommandExecution(final Host host) throws IOException, TimeoutException, InterruptedException {

        // Try to execute a 'cd /' shell command on the machine.
        // This is selected as a command that produces no output and doesn't require a functioning home directory.
        //FIXME add timeout; Timeout execption is never thrown
        final Process ssh_test_process = host.execute(MINIMAL_COMMAND);
        ssh_test_process.waitFor();
        ssh_test_process.destroy();
    }
}
