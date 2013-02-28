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
package uk.ac.standrews.cs.shabdiz.new_api;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;


/**
 * Presents a host on which a application can be deployed. Provides utility methods to deploy an application.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Host {

    /**
     * Uploads a given file or directory on the local platform to this host at the given destination.
     * 
     * @param source the source
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void upload(File source, String destination) throws IOException;

    /**
     * Uploads a given collection of files or directories on the local platform to this host at the given destination.
     * 
     * @param sources the sources
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void upload(final Collection<File> sources, final String destination) throws IOException;

    /**
     * Downloads a given file or directory from this host to the local platform.
     * 
     * @param source the source
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void download(String source, File destination) throws IOException;

    /**
     * Executes the given command and arguments on this host.
     * 
     * @param command the command and its arguments
     * @return the process that was started as the result of executing this command
     * @throws IOException Signals that an I/O exception has occurred.
     * @see ProcessBuilder#
     */
    Process execute(String... command) throws IOException;

    /**
     * Gets the platform.
     * 
     * @return the platform
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Platform getPlatform() throws IOException;

    /**
     * Gets the address of this host.
     * 
     * @return the address of this host
     */
    InetAddress getAddress();

    /**
     * Checks if this host represents the local platform.
     * 
     * @return true, if is local
     */
    boolean isLocal();

    /** Shuts down this host and closes any open resources. */
    void shutdown();

}
