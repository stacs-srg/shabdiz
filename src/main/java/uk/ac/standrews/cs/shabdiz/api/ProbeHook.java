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
package uk.ac.standrews.cs.shabdiz.api;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see DeployableNetwork
 */
public interface ProbeHook extends Closeable {

    /**
     * Attempts to probe whether this application is {@link ApplicationState#RUNNING running}.
     * 
     * @throws Exception if the application is not {@link ApplicationState#RUNNING running}
     */
    void ping() throws Exception;

    /**
     * Gets the last cached {@link ApplicationState state} of this application instance.
     * 
     * @return the last cached {@link ApplicationState state} of this application instance
     */
    ApplicationState getApplicationState();

    /**
     * Sets the {@link ApplicationState state} of this application instance.
     * 
     * @param state the new {@link ApplicationState state} of this application instance
     */
    void setApplicationState(ApplicationState state);

    /**
     * Closes this hook and any streams that are used by this hook.
     * If the hook is already closed then invoking this method has no effect.
     */
    @Override
    void close() throws IOException;
}
