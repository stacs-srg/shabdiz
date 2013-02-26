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
package uk.ac.standrews.cs.shabdiz.interfaces;

import java.util.List;
import java.util.Set;

import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.util.URL;

/**
 * Provides remote management hooks for some application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ApplicationManager {

    /**
     * Returns the name of this application.
     * 
     * @return the name of this application
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
     * Returns a list of scanners for this application.
     * 
     * @return a list of scanners, or {@code null} if none are required.
     */
    List<HostScanner> getHostScanners();

    /**
     * Gets the application library URLs.
     * 
     * @return the application library URLs
     */
    Set<URL> getApplicationLibraryURLs();

    /**
     * Sets the application library URLs.
     * 
     * @param urls the new application library URLs
     */
    void setApplicationLibraryURLs(Set<URL> urls);

    /**
     * Shuts down the manager.
     */
    void shutdown();
}
