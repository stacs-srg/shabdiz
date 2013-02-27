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

import java.net.UnknownHostException;
import java.util.SortedSet;

import uk.ac.standrews.cs.shabdiz.HostDescriptor;

/**
 * Madface remote application management interface.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface MadfaceManager {

    /**
     * Gets the application manager.
     * 
     * @return the application manager
     */
    ApplicationManager getApplicationManager();

    /**
     * Configures the application to be monitored.
     * 
     * @param application_manager the manager for the application
     */
    void setApplicationManager(ApplicationManager application_manager);

    /**
     * Sets a specified preference flag.
     * 
     * @param pref_name the name of the preference to be set
     * @param enabled true if the preference should be enabled
     * @return a status message
     */
    String setPrefEnabled(String pref_name, boolean enabled);

    /**
     * Adds a host.
     * 
     * @param host_descriptor the host descriptor
     */
    void add(HostDescriptor host_descriptor);

    /**
     * Drops a specified host from the manager.
     * 
     * @param host_descriptor the host descriptor
     */
    void drop(HostDescriptor host_descriptor);

    /**
     * Attempts to deploy the application to a specified host.
     * 
     * @param host_descriptor the host descriptor
     * @throws Exception if the attempt fails
     */
    void deploy(HostDescriptor host_descriptor) throws Exception;

    /**
     * Attempts to kill the application on a specified host. If kill_all_instances is true then all remote processes appearing to match the application are killed.
     * Otherwise, only known process handles for the application are killed. The latter option is more conservative in that will avoid accidentally killing other
     * undetected instances of the application; however it will only work if the application was instantiated by this manager.
     * 
     * @param host_descriptor the host descriptor
     * @param kill_all_instances true if all instances of the application on the host should be killed
     * @throws Exception if the attempt fails
     */
    void kill(HostDescriptor host_descriptor, boolean kill_all_instances) throws Exception;

    /**
     * Drops all hosts from the manager.
     */
    void dropAll();

    /**
     * Attempts to start the application on all hosts and waits until this appears to have succeeded.
     * 
     * @throws Exception if the attempt fails
     */
    void deployAll() throws Exception;

    /**
     * Attempts to kill the application an all hosts and waits until this appears to have succeeded.
     * 
     * @param kill_all_instances interpreted as for {@link #kill(HostDescriptor, boolean)}
     * @throws Exception if the attempt fails
     */
    void killAll(boolean kill_all_instances) throws Exception;

    /**
     * Blocks until a given host is in a specified state.
     * 
     * @param host_descriptor the host descriptor
     * @param state_to_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForHostToReachState(HostDescriptor host_descriptor, State state_to_reach) throws InterruptedException;

    /**
     * Blocks until a given host is not in a specified state.
     * 
     * @param host_descriptor the host descriptor
     * @param state_to_not_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForHostToReachStateThatIsNot(HostDescriptor host_descriptor, State state_to_not_reach) throws InterruptedException;

    /**
     * Blocks until all hosts are in a specified state.
     * 
     * @param state_to_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForAllToReachState(State state_to_reach) throws InterruptedException;

    /**
     * Blocks until all hosts are not in a specified state.
     * 
     * @param state_to_not_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForAllToReachStateThatIsNot(State state_to_not_reach) throws InterruptedException;

    /**
     * Sets whether auto-deployment should be performed. If enabled, an attempt is made to start the application on any host on which the application is not detected.
     * 
     * @param auto_deploy true if auto-deployment should be performed
     */
    void setAutoDeploy(boolean auto_deploy);

    /**
     * Sets whether auto-kill should be performed. If enabled, an attempt is made to kill the application on any host on which the application is detected.
     * 
     * @param auto_kill true if auto-kill should be performed
     */
    void setAutoKill(boolean auto_kill);

    /**
     * Sets whether auto-drop should be performed. If enabled, any unreachable hosts are dropped from the manager.
     * 
     * @param auto_drop true if auto-auto_drop should be performed
     */
    void setAutoDrop(boolean auto_drop);

    /**
     * Sets whether host scanning should be performed.
     * 
     * @param enabled true if host scanning should be performed
     */
    void setHostScanning(boolean enabled);

    /**
     * Gets the host descriptor for a given host.
     * 
     * @param host_name a host
     * @return the host descriptor for the host
     * @throws UnknownHostException if the host is not known by the manager
     */
    HostDescriptor findHostDescriptorByName(String host_name) throws UnknownHostException;

    /**
     * Gets the host descriptors for all currently managed hosts.
     * 
     * @return the host descriptors
     */
    SortedSet<HostDescriptor> getHostDescriptors();

    /**
     * Shuts down the manager.
     */
    void shutdown();
}
