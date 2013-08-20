/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
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
package uk.ac.standrews.cs.shabdiz.job;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.mashti.jetson.exception.RPCException;

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
     * @return the unique identifier of the submitted job on this worker
     * @throws RPCException signals that an RPC error has occured
     * @see ExecutorService#submit(Callable)
     */
    UUID submit(Job<? extends Serializable> job) throws RPCException;

    /**
     * Attempts to cancel execution of the task that is identified by the given {@code job_id}.
     * This attempt will fail if the task has already completed, has already been cancelled, or the given {@code job_id} is not recognised by this worker.
     * If successful, and this task has not started when cancel is called, this task will never run.
     * If the task has already started, then the {@code may_interrupt} parameter determines whether the thread executing this task should be interrupted in an attempt to stop the task.
     *
     * @param job_id the identifier of the job to be cancelled
     * @param may_interrupt whether the thread executing this task should be interrupted, or the in-progress task shuld be allowed to complete
     * @return {@code false} if the task could not be cancelled, typically because it has already completed normally; {@code true} otherwise
     * @throws UnknownJobException if the given job id is not recognised by this worker
     * @throws RPCException signals that an RPC error has occured
     * @see Future#cancel(boolean)
     */
    boolean cancel(UUID job_id, final boolean may_interrupt) throws RPCException;

    /**
     * Shuts down this worker.
     *
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}
