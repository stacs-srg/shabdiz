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

/**
 * Presents a probe hook that is capable of deploying an application instance on a {@link Host host}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see DeployableNetwork
 */
public interface DeployHook extends ProbeHook {

    /**
     * Deploys the instance of some application that is presented by this hook.
     * 
     * @throws Exception if deployment fails
     */
    void deploy() throws Exception;

    /**
     * Terminates the application instance that is presented by this hook.
     * 
     * @throws Exception if termination fails
     */
    void kill() throws Exception;

    /**
     * Gets the host on which the application instance is to run or running.
     * 
     * @return the host on which the application instance is to run or running
     */
    Host getHost();
}
