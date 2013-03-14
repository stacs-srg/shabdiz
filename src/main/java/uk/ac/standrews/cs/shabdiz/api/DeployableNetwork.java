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
package uk.ac.standrews.cs.shabdiz.api;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Maintains a set of {@link DeployHook hooks} across multiple {@link Host hosts} which are capable of deploying an application instance.
 * 
 * @param <T> the type of {@link ProbeHook instances} that are maintained by this network
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface DeployableNetwork<T extends DeployHook> extends ProbeNetwork<T> {

    /**
     * Gets the name of this application.
     * 
     * @return the name of this application
     */
    String getName();

    /**
     * Sets the auto kill enabled.
     * 
     * @param enabled the new auto kill enabled
     */
    void setAutoKillEnabled(boolean enabled);

    /**
     * Sets the auto deploy enabled.
     * 
     * @param enabled the new auto deploy enabled
     */
    void setAutoDeployEnabled(boolean enabled);

    /**
     * Sets the auto remove enabled.
     * 
     * @param enabled the new auto remove enabled
     */
    void setAutoRemoveEnabled(boolean enabled);

    /**
     * Adds a host to this network.
     * 
     * @param host the host
     * @return true, if successful
     */
    boolean add(Host host);

    /**
     * Attempts to deploy an application instance and sets the {@link ProbeHook#getApplicationReference() application reference} of the given application descriptor.
     * 
     * @param application_descriptor the application_descriptor
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException if interrupted while waiting for deployment to complete
     * @throws TimeoutException if the deployment times out
     */
    void deploy(T application_descriptor) throws IOException, InterruptedException, TimeoutException;

    /**
     * Attempts to {@link #deploy(ProbeHook) deploy} the application instances that are maintained by this network.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException if interrupted while waiting for deployment to complete
     * @throws TimeoutException if the deployment times out
     */
    void deployAll() throws IOException, InterruptedException, TimeoutException;

    /**
     * Attempts to {@link Process#destroy() terminate} the {@link ProbeHook#getProcesses() processes} of the given {@code application_descriptor}.
     * 
     * @param application_descriptor the application instance to terminate
     */
    void kill(T application_descriptor);

    /**
     * Attempts to {@link #kill(ProbeHook) terminate} all the application instances that their {@link ProbeHook#getHost() host} is equal to the given {@code host}.
     * 
     * @param host the host on which to terminate the application instances
     */
    void killAllOnHost(Host host);

    /**
     * Attempts to terminate all the application instances that are managed by this network.
     */
    void killAll();

    /**
     * {@inheritDoc}
     * Attempts to kill all application processes and {@link Host#close() close} the hosts of application instances.
     * 
     */
    @Override
    void shutdown();
}
