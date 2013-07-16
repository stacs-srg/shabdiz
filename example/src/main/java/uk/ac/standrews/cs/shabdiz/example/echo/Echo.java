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
package uk.ac.standrews.cs.shabdiz.example.echo;

import org.mashti.jetson.exception.RPCException;

/**
 * Presents the Echo remote interface.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Echo {

    /**
     * Echos a given message.
     *
     * @param message the message to echo
     * @return the echoed message
     * @throws RPCException signals that an RPC exception has occured
     */
    String echo(String message) throws RPCException;

    /**
     * Shuts down this remote interface.
     *
     * @throws RPCException signals that an RPC exception has occured
     */
    void shutdown() throws RPCException;
}
