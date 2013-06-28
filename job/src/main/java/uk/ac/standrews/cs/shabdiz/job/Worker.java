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

import com.staticiser.jetson.exception.RPCException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Provides a service to execute one or more asynchronous {@link Job jobs}. After {@link #shutdown() shutdown} any job submission with be rejected.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Worker extends Comparable<Worker> {

    /**
     * Gets the address on which this worker is exposed.
     *
     * @return the address on which this worker is exposed
     */
    InetSocketAddress getAddress();

    /**
     * Submits a value-returning task for execution to this worker and returns the pending result of the job.
     *
     * @param <Result> the type of pending result
     * @param job the job to submit
     * @return the pending result of the job
     * @throws RPCException if unable to make the remote call
     * @see Future
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    <Result extends Serializable> Future<Result> submit(Job<Result> job) throws RPCException;

    /**
     * Attempts to {@link Thread#interrupt() interrupt} any executing jobs. After this method is called any future job submission with be rejected.
     *
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}
