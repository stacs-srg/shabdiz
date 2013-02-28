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

import uk.ac.standrews.cs.shabdiz.new_api.Host;


/**
 * Launches workers on hosts.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Launcher {

    /**
     * Deploys worker on a described host and returns the reference to the deployed worker.
     * This method blocks until the worker is deployed.
     * 
     * @param host the descriptor of the host on which a worker is deployed
     * @return the reference to the deployed worker
     * @throws Exception if the attempt to deploy worker on host fails
     */
    Worker deployWorkerOnHost(Host host) throws Exception;

    /**
     * Shuts down this launcher. This method does <i>not</i> shot down any workers deployed by this launcher.
     * User may shot down workers by calling {@link Worker#shutdown()}.
     */
    void shutdown();
}
