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

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents a remote exception on a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class RemoteWorkerException extends RPCException {

    private static final long serialVersionUID = 4506906155644771341L;

    /**
     * Instantiates a new remote worker exception.
     *
     * @param message the message
     */
    RemoteWorkerException(final String message) {

        super(message);
    }

    /**
     * Instantiates a new remote worker exception from a given cause.
     *
     * @param cause the cause
     */
    RemoteWorkerException(final Throwable cause) {

        super(cause);
    }
}
