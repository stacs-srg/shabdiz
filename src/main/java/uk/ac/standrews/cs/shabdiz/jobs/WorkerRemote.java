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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;

/**
 * Presents a special type of worker which is deployed by {@link WorkerNetwork}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface WorkerRemote {

    /**
     * Submits a value-returning task for execution to a remote worker and returns the pending result of the task.
     * 
     * @param job the job to submit
     * @return the globally unique id of the submitted job
     * @throws RPCException if unable to make the remote call
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    UUID submitJob(JobRemote job) throws JsonRpcException;

    /**
     * Cancels a given job exexuting on this worker.
     * 
     * @param job_id the job_id
     * @param may_interrupt the may_interrupt
     * @return true, if successful
     * @throws JsonRpcException the json rpc exception
     */
    boolean cancel(UUID job_id, final boolean may_interrupt) throws JsonRpcException;

    /**
     * Shuts down this worker.
     * 
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws JsonRpcException;
}