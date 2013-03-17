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
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

public abstract class AbstractApplicationManager implements ApplicationManager {

    private static final Duration SSH_MINIMAL_COMMAND_EXECUTION_TIMEOUT = new Duration(5, TimeUnit.SECONDS);

    // A minimal shell command that will be attempted in order to check ssh connectivity. Chosen to have minimal dependency on execution environment, so doesn't rely on anything specific to a user.
    private static final String MINIMAL_COMMAND = "cd /"; //FIXME is this platform dependent?

    protected abstract void attemptApplicationCall(ApplicationDescriptor descriptor) throws Exception;

    @Override
    public ApplicationState probeApplicationState(final ApplicationDescriptor descriptor) {

        ApplicationState state;
        try {
            state = getStateFromApplicationReference(descriptor);
        }
        catch (final Exception e) { //Catch Exception to cover NPE since application reference may be null
            final Host host = descriptor.getHost();
            state = getStateFromHost(host);
        }
        return state;
    }

    private ApplicationState getStateFromHost(final Host host) {

        //TODO tidy up
        try {
            attemptAddressResolution(host.getAddress().getHostName());
            // Application call failed, so try SSH connection.
            attemptCommandExecution(host);
            return ApplicationState.AUTH;
        }
        catch (final UnknownHostException e) {

            // Machine address couldn't be resolved.
            return ApplicationState.INVALID;
        }
        catch (final SSHException e) {

            // Couldn't make SSH connection with specified credentials.
            return ApplicationState.NO_AUTH;
        }
        catch (final IOException e) {

            // Network error trying to make SSH connection.
            return ApplicationState.UNREACHABLE;
        }
        catch (final TimeoutException e) {

            // SSH connection timed out.
            return ApplicationState.UNREACHABLE;
        }
        catch (final InterruptedException e) {

            // SSH connection timed out.
            return ApplicationState.UNREACHABLE;
        }
        catch (final Throwable e) {
            // Treat any other exception as unreachable state
            return ApplicationState.UNREACHABLE;
        }
    }

    private void attemptAddressResolution(final String host_name) throws UnknownHostException {

        InetAddress.getByName(host_name);
    }

    private ApplicationState getStateFromApplicationReference(final ApplicationDescriptor descriptor) throws Exception {

        attemptApplicationCall(descriptor);
        return ApplicationState.RUNNING;
    }

    private void attemptCommandExecution(final Host host) throws Throwable {

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
            }, SSH_MINIMAL_COMMAND_EXECUTION_TIMEOUT);
        }
        catch (final ExecutionException e) {
            throw e.getCause();
        }
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        final Iterator<Process> process_iterator = descriptor.getProcesses().iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
            process_iterator.remove();
        }
    }
}
