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
package uk.ac.standrews.cs.shabdiz;

import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * The possible states of an application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see ApplicationDescriptor
 * @see StatusScanner
 */
public enum ApplicationState {

    /** The application is deployed. */
    DEPLOYED,

    /** The machine can be contacted but the application is not running; the machine will accept a connection with the given credentials. */
    AUTH,

    /** The {@link Host host} address is not a resolvable IP address. */
    INVALID,

    /** The machine can be contacted, the application is running, and an attempt has been recently made to kill the application. */
    KILLED,

    /** The machine can be contacted and an attempt has been recently made to launch the application on it. */
    LAUNCHED,

    /** The machine can be contacted but the application is not running; the machine will not accept an SSH connection with the given credentials. */
    NO_AUTH,

    /** The machine can be contacted and the application is running. */
    RUNNING,

    /** The address is resolvable as an IP address but the application cannot be contacted. */
    UNREACHABLE,

    /** The initial state. */
    UNKNOWN;
}
