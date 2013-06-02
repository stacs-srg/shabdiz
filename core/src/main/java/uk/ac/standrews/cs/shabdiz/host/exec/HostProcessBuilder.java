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
package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.IOException;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Starts a {@link Process process} on a given {@link Host host}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface HostProcessBuilder {

    /**
     * Starts a {@link Process process} on a given {@code host}.
     *
     * @param host the host on which to start a process
     * @param parameters the parameters to pass to process
     * @return the process running on the host
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Process start(Host host, String... parameters) throws IOException;
}
