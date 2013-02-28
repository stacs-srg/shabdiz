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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;

/**
 * Describes a managed application instance on a {@link Host host}.
 * Provides references to the processes that belong to this application, a {@link Pingable reference} to the application and its {@link ApplicationState state}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see ApplicationNetwork
 */
public interface ApplicationDescriptor {

    /**
     * Add a PropertyChangeListener for a specific property.
     * If {@code property_name} or listener is {@code null} no exception is thrown and no action is taken.
     * 
     * @param property_name The name of the property to listen on
     * @param listener the listener to be added
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    void addPropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If {@code property_name} is null, no exception is thrown and no action is taken.
     * If listener is {@code null} or was never added for the specified property, no exception is thrown and no action is taken.
     * 
     * @param property_name The name of the property that was listened on
     * @param listener the listener to be removed
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    void removePropertyChangeListener(String property_name, PropertyChangeListener listener);

    /**
     * Gets the host on which the application instance is to be deployed.
     * 
     * @return the host on which the application instance is to be deployed.
     */
    Host getHost();

    /**
     * Gets the application reference.
     * 
     * @return the application reference
     */
    Pingable getApplicationReference();

    /**
     * Gets the processes that are started on this descriptor's {@link Host}.
     * 
     * @return the processes that are started on this descriptor's {@link Host}.
     */
    Collection<Process> getProcesses();

    /**
     * Gets the state of this application.
     * 
     * @return the state of this application.
     */
    ApplicationState getState();

    /**
     * Sets the state of this application.
     * 
     * @param state the new applciation state
     */
    void setState(ApplicationState state);
}
