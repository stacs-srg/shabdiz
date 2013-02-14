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
package uk.ac.standrews.cs.nds.madface.interfaces;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.madface.Credentials;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.Patterns;
import uk.ac.standrews.cs.nds.madface.URL;

/**
 * Madface remote application management interface.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface IMadfaceManager {

    /**
     * Configures the application to be monitored.
     * 
     * @param application_manager_class the application manager class
     * @param application_urls the URLs for for the application's jar and library files
     * 
     * @throws IllegalAccessException if the application manager cannot be instantiated
     * @throws InstantiationException if the application manager cannot be instantiated
     */
    void configureApplication(Class<? extends IApplicationManager> application_manager_class, Set<URL> application_urls) throws InstantiationException, IllegalAccessException;

    /**
     * Configures the application to be monitored.
     * 
     * @param application_manager_class the application manager class
     * @param url_base the common base for the application's jar and library URLs
     * @param jar_names the names of the jar files used by the application
     * @param lib_names the names of the library files used by the application
     * 
     * @throws IOException if an application URL cannot be formed, or a connection to an application URL cannot be established
     * @throws IllegalAccessException if the application manager cannot be instantiated
     * @throws InstantiationException if the application manager cannot be instantiated
     */
    void configureApplication(Class<? extends IApplicationManager> application_manager_class, URL url_base, Set<String> jar_names, Set<String> lib_names) throws IOException, InstantiationException, IllegalAccessException;

    /**
     * Configures the application to be monitored.
     * 
     * @param application_manager the manager for the application
     */
    void configureApplication(IApplicationManager application_manager);

    /**
     * Configures the application to be monitored.
     * 
     * <p>Application URLs must be added before host descriptors or added, or they must already be included in the host descriptors that are added.
     * 
     * @param application_urls the URLs for for the application's jar and library files
     */
    void configureApplication(Set<URL> application_urls);

    /**
     * Sets a specified preference flag.
     * 
     * @param pref_name the name of the preference to be set
     * @param enabled true if the preference should be enabled
     * @return a status message
     */
    String setPrefEnabled(String pref_name, boolean enabled);

    /**
     * Returns the library URLs used by the configured application.
     * 
     * @return the library URLs used by the configured application
     */
    Set<URL> getApplicationUrls();

    /**
     * Returns the entrypoint class for the configured application.
     * 
     * @return the entrypoint class for the configured application
     */
    Class<? extends IApplicationManager> getApplicationEntrypoint();

    /**
     * Returns the name for the configured application.
     * 
     * @return the name for the configured application
     */
    String getApplicationName();

    /**
     * Adds the set of hosts specified by a given address pattern.
     * 
     * @param multi_line_host_patterns a newline-separated list, each entry containing a single host address or a pattern as defined by {@link Patterns#resolveHostPattern}
     * @param credentials the credentials to be used with the hosts
     */
    void add(String multi_line_host_patterns, Credentials credentials);

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
    void waitForHostToReachState(HostDescriptor host_descriptor, HostState state_to_reach) throws InterruptedException;

    /**
     * Blocks until a given host is not in a specified state.
     * 
     * @param host_descriptor the host descriptor
     * @param state_to_not_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForHostToReachStateThatIsNot(HostDescriptor host_descriptor, HostState state_to_not_reach) throws InterruptedException;

    /**
     * Blocks until all hosts are in a specified state.
     * 
     * @param state_to_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForAllToReachState(HostState state_to_reach) throws InterruptedException;

    /**
     * Blocks until all hosts are not in a specified state.
     * 
     * @param state_to_not_reach the state
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForAllToReachStateThatIsNot(HostState state_to_not_reach) throws InterruptedException;

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
     * @param host a host
     * @return the host descriptor for the host
     * @throws UnknownHostException if the host is not known by the manager
     */
    HostDescriptor getHostDescriptor(String host) throws UnknownHostException;

    /**
     * Gets the host descriptors for all currently managed hosts.
     * 
     * @return the host descriptors
     */
    SortedSet<HostDescriptor> getHostDescriptors();

    /**
     * Adds a callback for host status changes.
     * 
     * @param host_status_callback the callback
     */
    void addHostStatusCallback(IHostStatusCallback host_status_callback);

    /**
     * Adds a callback for attribute changes.
     * 
     * @param attributes_callback the callback
     */
    void addAttributesCallback(IAttributesCallback attributes_callback);

    /**
     * Shuts down the manager.
     */
    void shutdown();
}
