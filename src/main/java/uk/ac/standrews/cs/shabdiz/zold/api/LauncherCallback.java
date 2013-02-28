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
package uk.ac.standrews.cs.shabdiz.zold.api;

import java.io.Serializable;
import java.util.UUID;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Receives notifications from workers about the outcome of a submitted job.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface LauncherCallback {

    /**
     * Notifies the launcher about the result of a completed job.
     *
     * @param job_id the globally unique id of the submitted job
     * @param result the result of the completed job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyCompletion(UUID job_id, Serializable result) throws RPCException;

    /**
     * Notifies the launcher about the exception resulted by executing a job.
     *
     * @param job_id the globally unique id of the submitted job
     * @param exception the exception which occurred when trying to execute a job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyException(UUID job_id, Exception exception) throws RPCException;
}
