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

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see DeployableNetwork
 */
public interface ApplicationDescriptor {

    /**
     * Gets the application manager.
     * 
     * @return the application manager
     */
    ApplicationManager getApplicationManager();

    /**
     * Gets the last cached {@link ApplicationState state} of this application instance.
     * 
     * @return the last cached {@link ApplicationState state} of this application instance
     */
    ApplicationState getCachedApplicationState();

    /**
     * Sets the {@link ApplicationState state} of this application instance.
     * 
     * @param state the new {@link ApplicationState state} of this application instance
     */
    void setCachedApplicationState(ApplicationState state);

    /**
     * Gets the host on which the application instance is to run or running.
     * 
     * @return the host on which the application instance is to run or running
     */
    Host getHost();
}
