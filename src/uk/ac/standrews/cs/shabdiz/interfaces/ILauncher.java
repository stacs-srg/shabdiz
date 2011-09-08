/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.interfaces;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;

/**
 * Deploys worker on host.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ILauncher {

    /**
     * Deploys worker on a described host and returns the reference to the deployed worker.
     * This method blocks until the worker is deployed.
     *
     * @param host_descriptor the descriptor of the host on which a worker is deployed
     * @return the reference to the deployed worker
     * @throws Exception if the attempt to deploy worker on host fails
     */
    IWorker deployWorkerOnHost(HostDescriptor host_descriptor) throws Exception;

    /**
     * Shuts down this coordinator. This method does <code>not</code> shot down any workers deployed by this coordinator.
     * User may shot down workers by calling {@link IWorker#shutdown()}.
     */
    void shutdown();
}
