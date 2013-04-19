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
package uk.ac.standrews.cs.shabdiz.jobs;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;

/**
 * Presents a remote exception on a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class RemoteWorkerException extends JsonRpcException {

    private static final long serialVersionUID = 4506906155644771341L;

    /**
     * Instantiates a new remote worker exception.
     * 
     * @param message the message
     */
    RemoteWorkerException(final String message) {

        super(-3999, message);
    }

    /**
     * Instantiates a new remote worker exception from a given cause.
     * 
     * @param cause the cause
     */
    RemoteWorkerException(final Throwable cause) {

        super(-3999, cause);
    }
}
