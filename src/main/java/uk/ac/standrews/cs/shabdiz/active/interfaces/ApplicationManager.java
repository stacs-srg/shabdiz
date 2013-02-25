/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
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
package uk.ac.standrews.cs.shabdiz.active.interfaces;

import java.util.List;

import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;

/**
 * Provides remote management hooks for some application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface ApplicationManager {

    /**
     * Returns the application's name, as used in management user interface.
     * 
     * @return the application's name
     */
    String getApplicationName();

    /**
     * Attempts to make some call to the application running on some remote host.
     * This is used by management software to decide whether the application appears to be running on a given host.
     * This method should set the application reference using {@link HostDescriptor#applicationReference(Object)} to
     * the successfully established connection, or to null if the attempt fails.
     * 
     * @param host_descriptor a descriptor for a remote host
     * @throws Exception if the attempt to call the application fails
     */
    void attemptApplicationCall(HostDescriptor host_descriptor) throws Exception;

    /**
     * Attempts to deploy a new instance of the application on some remote host.
     * This method may call {@link HostDescriptor#process(Process)} if
     * the application is successfully deployed.
     * 
     * @param host_descriptor a descriptor for a remote host
     * @throws Exception if the attempted application deployment fails
     */
    void deployApplication(HostDescriptor host_descriptor) throws Exception;

    /**
     * Attempts to kill the instance of the application on some remote host.
     * 
     * @param host_descriptor a descriptor for a remote host
     * @param kill_all_instances interpreted as for {@link MadfaceManager#kill(HostDescriptor, boolean)}
     * @throws Exception if the attempt to kill the application fails
     */
    void killApplication(HostDescriptor host_descriptor, boolean kill_all_instances) throws Exception;

    /**
     * Returns a list of single-host scanners for the application.
     * 
     * @return a list of single-host scanners, or null if none are required.
     */
    List<SingleHostScanner> getSingleScanners();

    /**
     * Returns a list of global scanners for the application.
     * 
     * @return a list of global scanners, or null if none are required.
     */
    List<GlobalHostScanner> getGlobalScanners();

    /**
     * Shuts down the manager.
     */
    void shutdown();
}
