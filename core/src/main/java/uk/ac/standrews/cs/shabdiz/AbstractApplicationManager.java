/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
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

import com.jcraft.jsch.JSchException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

/**
 * Implements common state probe and termination functionality.
 * This class probes the state of a given {@link ApplicationDescriptor descriptor} by attempting an application-specific call.
 * If the call fails, attempts to probe the state of the descriptor's {@link ApplicationDescriptor#getHost() host} by executing a {@code change directory} command.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractApplicationManager implements ApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationManager.class);
    private static final Duration DEFAULT_COMMAND_EXECUTION_TIMEOUT = new Duration(15, TimeUnit.SECONDS);
    private final Duration command_execution_timeout;

    /** Instantiates a new application manager with the default command execution timeout of {@code 15} seconds. */
    protected AbstractApplicationManager() {

        this(DEFAULT_COMMAND_EXECUTION_TIMEOUT);
    }

    /**
     * Instantiates a new application manager and sets the command execution timeout to the given {@code command_execution_timeout}.
     *
     * @param command_execution_timeout the command execution timeout
     */
    protected AbstractApplicationManager(final Duration command_execution_timeout) {

        this.command_execution_timeout = command_execution_timeout;
    }

    @Override
    public ApplicationState probeState(final ApplicationDescriptor descriptor) {

        try {
            return probeApplicationState(descriptor);
        }
        catch (final Exception e) {
            LOGGER.debug("state probe using application call failed", e);
            final Host host = descriptor.getHost();
            return probeHostState(host);
        }
    }

    protected ApplicationState probeApplicationState(final ApplicationDescriptor descriptor) throws Exception {

        attemptApplicationCall(descriptor);
        return ApplicationState.RUNNING;
    }

    protected abstract void attemptApplicationCall(ApplicationDescriptor descriptor) throws Exception;

    private ApplicationState probeHostState(final Host host) {

        ApplicationState state;
        if (host == null) {
            state = ApplicationState.UNKNOWN;
        }
        else {
            try {
                resolveAddressByHostName(host.getName());
                checkAuthorityByCommandExecution(host);
                state = ApplicationState.AUTH;
            }
            catch (final Throwable e) {
                LOGGER.debug("attempting to resolve state by exception", e);
                state = resolveStateFromThrowable(e);
            }
        }
        assert state != null;
        return state;
    }

    protected ApplicationState resolveStateFromThrowable(final Throwable throwable) {

        final ApplicationState state;
        if (throwable == null) {
            state = ApplicationState.UNKNOWN;
        }
        else if (throwable instanceof UnknownHostException) {
            state = ApplicationState.INVALID;
        }
        else if (throwable instanceof JSchException) {
            state = ApplicationState.NO_AUTH;
        }
        else if (throwable instanceof TimeoutException) {
            state = ApplicationState.UNREACHABLE;
        }
        else if (throwable instanceof InterruptedException) {
            state = ApplicationState.UNKNOWN;
        }
        else {
            final Throwable cause = throwable.getCause();
            state = cause == null ? ApplicationState.UNREACHABLE : resolveStateFromThrowable(cause);
        }
        return state;
    }

    private void checkAuthorityByCommandExecution(final Host host) throws Throwable {

        LOGGER.debug("attemting to execute a minimal command on {} to check for authority with the timeout {}", host.getName(), command_execution_timeout);
        TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                final String cd_command = Commands.CHANGE_DIRECTORY.get(host.getPlatform(), ".");
                LOGGER.trace("checking authority on host {} by executing {}", host, cd_command);
                final Process cd_process = host.execute(cd_command);
                LOGGER.trace("awaiting normal termination of authority check command on host {}", host);
                final String cd_execution_output = ProcessUtil.awaitNormalTerminationAndGetOutput(cd_process);
                LOGGER.trace("authority check command output on host {}: {}", host, cd_execution_output);
                return null; // Void task.
            }
        }, command_execution_timeout);
    }

    private static void resolveAddressByHostName(final String host_name) throws UnknownHostException {

        LOGGER.debug("attempting to resolve address from host name {}", host_name);
        InetAddress.getByName(host_name);
    }
}
