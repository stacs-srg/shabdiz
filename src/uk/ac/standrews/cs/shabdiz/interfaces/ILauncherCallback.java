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
import java.util.UUID;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Receives notifications from workers about the outcome of a submitted job.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ILauncherCallback {

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
