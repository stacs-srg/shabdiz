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
package uk.ac.standrews.cs.shabdiz.worker.rpc;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents a remote exception on a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RemoteWorkerException extends RPCException {

    private static final long serialVersionUID = 4506906155644771341L;

    /**
     * Instantiates a new remote worker exception.
     *
     * @param message the message
     */
    public RemoteWorkerException(final String message) {

        super(message);
    }

    /**
     * Instantiates a new remote worker exception from a given cause.
     *
     * @param cause the cause
     */
    public RemoteWorkerException(final Throwable cause) {

        super(cause);
    }
}
