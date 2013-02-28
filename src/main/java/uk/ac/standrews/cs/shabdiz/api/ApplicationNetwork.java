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

import java.util.Set;

import uk.ac.standrews.cs.shabdiz.zold.p2p.network.Network;

// TODO: Auto-generated Javadoc
/**
 * Maintains a set of {@link ApplicationDescriptor instances} across multiple {@link Host hosts}.
 * 
 * @param <T> the type of {@link ApplicationDescriptor instances} that are maintained by this network
 * @see Network
 * @see ApplicationDescriptor
 * @see Host
 */
public interface ApplicationNetwork<T extends ApplicationDescriptor> extends Set<T> {

    /**
     * Gets the application name.
     * 
     * @return the application name
     */
    String getApplicationName();

    /**
     * Causes the current thread to wait until all the {@link ApplicationDescriptor instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states to match any of
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    void awaitAnyOfStates(ApplicationState... states) throws InterruptedException;

    /**
     * Adds the given {@code scanner} to the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} has already been added.
     * 
     * @param scanner the scanner to add
     * @return true, if successfully added
     */
    boolean addScanner(Scanner<T> scanner);

    /**
     * Removes the given {@code scanner} from the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} does not exist in the collection of this network's scanners.
     * 
     * @param scanner the scanner to remove
     * @return true, if successfully removed
     */
    boolean removeScanner(Scanner<T> scanner);

    /**
     * Sets the policy on whether the scanners of this network should be {@link Scanner#setEnabled(boolean) enabled}.
     * 
     * @param enabled if {@code true} enables all the scanners of this network, disables all the scanners otherwise
     */
    void setScanEnabled(boolean enabled);

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
     * Deploy.
     * 
     * @param application_descriptor the application_descriptor
     */
    void deploy(T application_descriptor);

    /**
     * Deploy all.
     */
    void deployAll();

    /**
     * Kill.
     * 
     * @param application_descriptor the application_descriptor
     */
    void kill(T application_descriptor);

    /**
     * Kill all on host.
     * 
     * @param host the host
     */
    void killAllOnHost(Host host);

    /**
     * Kill all.
     */
    void killAll();

    /** Shuts down this network and closes any open resources. */
    void shutdown();
}
