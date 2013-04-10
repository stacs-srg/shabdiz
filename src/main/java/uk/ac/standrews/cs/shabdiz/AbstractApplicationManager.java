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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

/**
 * Provides default implementations of {@link ApplicationDescriptor} termination and state probe.
 * This class executes the SSH command '{@code cd /}' to determine the state of an {@link ApplicationDescriptor application descriptor's} host if the application call attempt fails.
 * The application-specific call is defined by {@link #attemptApplicationCall(ApplicationDescriptor)}. The application call is considered to have failed if the execution of this method results in {@link Exception}.
 * This class terminates a given {@link ApplicationDescriptor} by {@link Process#destroy() destroying} all of its {@link ApplicationDescriptor#getProcesses() processes}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractApplicationManager implements ApplicationManager {

    /** The Constant SSH_MINIMAL_COMMAND_EXECUTION_TIMEOUT. */
    private static final Duration SSH_MINIMAL_COMMAND_EXECUTION_TIMEOUT = new Duration(5, TimeUnit.SECONDS);

    // A minimal shell command that will be attempted in order to check ssh connectivity. Chosen to have minimal dependency on execution environment, so doesn't rely on anything specific to a user.
    private static final String MINIMAL_COMMAND = "cd /"; //FIXME is this platform dependent?

    private final Duration ssh_timeout;

    /**
     * Instantiates a new application manager with the default timeout of {@code 5 seconds} for executing a minimal SSH command.
     */
    protected AbstractApplicationManager() {

        this(SSH_MINIMAL_COMMAND_EXECUTION_TIMEOUT);
    }

    /**
     * Instantiates a new application manager and sets the SSH timeout to the given {@code ssh_timeout}.
     * 
     * @param ssh_timeout the ssh_timeout
     */
    protected AbstractApplicationManager(final Duration ssh_timeout) {

        this.ssh_timeout = ssh_timeout;
    }

    protected abstract void attemptApplicationCall(ApplicationDescriptor descriptor) throws Exception;

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        final Iterator<Process> process_iterator = descriptor.getProcesses().iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
            process_iterator.remove();
        }
    }

    @Override
    public ApplicationState probeApplicationState(final ApplicationDescriptor descriptor) {

        try {
            return probeStateByApplicationCall(descriptor);
        }
        catch (final Exception e) {
            //Catch Exception to cover NPE since application reference may be null
            return probeStateBySshCommandExecution(descriptor);
        }
    }

    private ApplicationState probeStateBySshCommandExecution(final ApplicationDescriptor descriptor) {

        final Host host = descriptor.getHost();
        try {
            attemptAddressResolution(host.getAddress().getHostName());
            attemptMinimalSshCommandExecution(host);
            return ApplicationState.AUTH;
        }
        catch (final UnknownHostException e) {

            return ApplicationState.INVALID; // Machine address couldn't be resolved.
        }
        catch (final SSHException e) {

            return ApplicationState.NO_AUTH; // Couldn't make SSH connection with specified credentials.
        }
        catch (final IOException e) {

            return ApplicationState.UNREACHABLE; // Network error trying to make SSH connection.
        }
        catch (final TimeoutException e) {

            return ApplicationState.UNREACHABLE; // SSH connection timed out.
        }
        catch (final InterruptedException e) {

            return ApplicationState.UNREACHABLE; // SSH connection was interrupted while waiting for completion.
        }
        catch (final Throwable e) {

            return ApplicationState.UNREACHABLE; // Treat any other exception as unreachable state
        }
    }

    private void attemptAddressResolution(final String host_name) throws UnknownHostException {

        InetAddress.getByName(host_name);
    }

    private ApplicationState probeStateByApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        attemptApplicationCall(descriptor);
        return ApplicationState.RUNNING;
    }

    private void attemptMinimalSshCommandExecution(final Host host) throws Throwable {

        // Try to execute a 'cd /' shell command on the machine.
        // This is selected as a command that produces no output and doesn't require a functioning home directory.
        try {
            TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    Process ssh_test_process = null;
                    try {
                        ssh_test_process = host.execute(MINIMAL_COMMAND);
                        ssh_test_process.waitFor();
                    }
                    finally {
                        if (ssh_test_process != null) {
                            ssh_test_process.destroy();
                        }
                    }
                    return null; // Void task.
                }
            }, ssh_timeout);
        }
        catch (final ExecutionException e) {
            throw e.getCause();
        }
    }
}
