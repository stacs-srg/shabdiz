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
package uk.ac.standrews.cs.shabdiz.impl;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;

/**
 * Implements a passive mechanism by which a {@link IWorker} can be contacted.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class Worker implements IWorker {

    private final InetSocketAddress worker_address;
    private final Launcher launcher;

    /**
     * Instantiates a new coordinated worker wrapper.
     *
     * @param worker_remote the worker remote to wrap
     * @param launcher the coordinator of the remote worker
     */
    Worker(final Launcher launcher, final InetSocketAddress worker_address) {

        this.worker_address = worker_address;
        this.launcher = launcher;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public <Result extends Serializable> Future<Result> submit(final IJobRemote<Result> job) throws RPCException {

        return launcher.submitJob(job, worker_address);
    }

    @Override
    public void shutdown() throws RPCException {

        launcher.shutdownWorker(worker_address);
    }
}
