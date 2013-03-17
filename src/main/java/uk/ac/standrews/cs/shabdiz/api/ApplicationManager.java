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

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;

// TODO: Auto-generated Javadoc
/**
 * The Interface ApplicationManager.
 * 
 * @author masih
 */
public interface ApplicationManager {

    /**
     * Update application state of the given descriptor by calling {@link ApplicationDescriptor#setCachedApplicationState(ApplicationState)}.
     * 
     * @param descriptor the descriptor
     * @return the application state
     */
    ApplicationState probeApplicationState(ApplicationDescriptor descriptor);

    /**
     * Kill.
     * 
     * @param descriptor the descriptor
     * @throws Exception the exception
     */
    void kill(ApplicationDescriptor descriptor) throws Exception;

    /**
     * Deploy.
     * 
     * @param descriptor the descriptor
     * @return the object
     * @throws Exception the exception
     */
    Object deploy(ApplicationDescriptor descriptor) throws Exception;

}
