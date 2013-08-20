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

import java.util.UUID;
import org.mashti.jetson.exception.RPCException;

/**
 * Signlas that a given {@link UUID job ID} is unknown by a {@link WorkerRemote remote worker}.
 * This exception is typically thrown by {@link WorkerRemote#cancel(UUID, boolean)} operation when the given job ID is not recognised by the {@link WorkerRemote remote worker}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class UnknownJobException extends RPCException {

    private static final long serialVersionUID = 8032891766959571488L;

    UnknownJobException(final String message) {

        super(message);
    }
}
