/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface;

/**
 * The possible states of a remotely managed host.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public enum HostState {

    /**
     * The machine can be contacted but the application is not running; the machine
     * will accept an SSH connection with the given credentials.
     */
    AUTH,

    DEPLOYED,

    /**
     * The address is not a resolvable IP address.
     */
    INVALID,

    /**
     * The machine can be contacted, the application is running, and an attempt has been recently made to kill the application.
     */
    KILLED,

    /**
     * The machine can be contacted and an attempt has been recently made to launch the application on it.
     */
    LAUNCHED,

    /**
     * The machine can be contacted but the application is not running; the machine
     * will not accept an SSH connection with the given credentials.
     */
    NO_AUTH,

    /**
     * The machine can be contacted and the application is running.
     */
    RUNNING,

    /**
     * The start state.
     */
    UNKNOWN,

    /**
     * The address is resolvable as an IP address but the machine cannot be contacted.
     */
    UNREACHABLE;
}
