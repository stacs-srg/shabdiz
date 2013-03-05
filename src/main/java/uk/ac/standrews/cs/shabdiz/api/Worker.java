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

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;

/**
 * Provides a service to execute one or more asynchronous {@link JobRemote jobs}. After {@link #shutdown() shutdown} any job submission with be rejected.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Worker extends Pingable, Comparable<Worker> {

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
    <Result extends Serializable> Future<Result> submit(JobRemote<Result> job) throws RPCException;

    /**
     * Attempts to {@link Thread#interrupt() interrupt} any executing jobs. After this method is called any future job submission with be rejected.
     * 
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}
