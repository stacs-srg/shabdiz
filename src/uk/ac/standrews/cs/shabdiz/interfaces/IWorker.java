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
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remote functionalities provided by a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IWorker {

    /**
     * Gets the address on which the worker is exposed.
     * 
     * @return the address on which the worker is exposed
     */
    InetSocketAddress getAddress();

    /**
     * Submits a value-returning task for execution to this worker and returns the pending result of the task.
     *
     * @param <Result> the type of pending result
     * @param job the job to submit
     * @return the pending result of the job
     * @throws RPCException if unable to make the remote call
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    <Result extends Serializable> Future<Result> submit(IJobRemote<Result> job) throws RPCException;

    /**
     * Shuts down this worker.
     * 
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}
