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
package uk.ac.standrews.cs.shabdiz.api;

import java.util.Set;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;

// TODO: Auto-generated Javadoc
/**
 * Maintains a set of {@link ApplicationDescriptor hooks} to application instances across multiple {@link Host hosts}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ApplicationNetwork extends Set<ApplicationDescriptor> {

    /**
     * Gets the name of this application.
     * 
     * @return the name of this application
     */
    String getApplicationName();

    /**
     * Attempts to deploy an application instance and sets the {@link ApplicationDescriptor#getApplicationReference() application reference} of the given application descriptor.
     * 
     * @param application_descriptor the application_descriptor
     * @throws Exception the exception
     */
    void deploy(ApplicationDescriptor application_descriptor) throws Exception;

    /**
     * Attempts to {@link #deploy(ApplicationDescriptor) deploy} the application instances that are maintained by this network.
     * 
     * @throws Exception the exception
     */
    void deployAll() throws Exception;

    /**
     * Attempts to {@link Process#destroy() terminate} the {@link ApplicationDescriptor#getProcesses() processes} of the given {@code application_descriptor}.
     * 
     * @param application_descriptor the application instance to terminate
     * @throws Exception the exception
     */
    void kill(ApplicationDescriptor application_descriptor) throws Exception;

    /**
     * Attempts to {@link #kill(ApplicationDescriptor) terminate} all the application instances that their {@link ApplicationDescriptor#getHost() host} is equal to the given {@code host}.
     * 
     * @param host the host on which to terminate the application instances
     * @throws Exception the exception
     */
    void killAllOnHost(Host host) throws Exception;

    /**
     * Attempts to terminate all the application instances that are managed by this network.
     * 
     * @throws Exception the exception
     */
    void killAll() throws Exception;

    /**
     * Causes the current thread to wait until all the {@link ApplicationDescriptor instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states which application instances must reach at least once
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    void awaitAnyOfStates(ApplicationState... states) throws InterruptedException;

    /**
     * Adds the given {@code scanner} to the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} has already been added.
     * 
     * @param scanner the scanner to add
     * @return true, if successfully added
     */
    boolean addScanner(Scanner scanner);

    /**
     * Removes the given {@code scanner} from the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} does not exist in the collection of this network's scanners.
     * 
     * @param scanner the scanner to remove
     * @return true, if successfully removed
     */
    boolean removeScanner(Scanner scanner);

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
     * Attempts to kill all application processes and {@link Host#close() close} the hosts of application instances.
     * Removes all the hooks that are maintained by this network.
     * After this method is called, this network is no longer usable.
     */
    void shutdown();
}
