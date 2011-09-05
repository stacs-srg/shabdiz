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

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remote functionalities provided by a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IWorkerRemote {

    /**
     * Submits a value-returning task for execution to a remote worker and returns the pending result of the task.
     *
     * @param <Result> the generic type
     * @param remote_job the job to submit
     * @return the reference to the pending result of the submitted task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the given remote job is <code>null</code>
     * @throws RPCException if unable to make the remote call
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    <Result extends Serializable> IFutureRemoteReference<Result> submit(IJobRemote<Result> remote_job) throws RPCException;

    /**
     * Shuts down this worker.
     * 
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}
