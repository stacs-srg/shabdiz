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

import java.util.Collection;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;

/**
 * Provides references to the {@link Process processes} that belong to an application instance running on a {@link Host host}, a {@link Pingable reference} to the application instance and its {@link ApplicationState state}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see ApplicationNetwork
 */
public interface ApplicationDescriptor {

    /**
     * Gets the host on which this application instance is to run or running.
     * 
     * @return the host on which the application instance is to be deployed.
     */
    Host getHost();

    /**
     * Attempts to probe whether this application is {@link ApplicationState#RUNNING running}.
     * 
     * @throws Exception if the application is not {@link ApplicationState#RUNNING running}
     * @see Pingable#ping()
     */
    void ping() throws Exception;

    /**
     * Gets the processes that are started on this instance's {@link #getHost() host}.
     * 
     * @return the processes that are started on this instance's {@link #getHost() host}
     */
    Collection<Process> getProcesses();

    /**
     * Gets the last cached {@link ApplicationState state} of this application instance.
     * 
     * @return the last cached {@link ApplicationState state} of this application instance
     */
    ApplicationState getApplicationState();

    /**
     * Sets the {@link ApplicationState state} of this application instance.
     * 
     * @param state the new {@link ApplicationState state} of this application instance
     */
    void setState(ApplicationState state);
}
