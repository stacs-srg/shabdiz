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
package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import uk.ac.standrews.cs.shabdiz.Platform;

/**
 * The Interface Host.
 */
public interface Host {

    /**
     * Upload.
     * 
     * @param source the source
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void upload(File source, String destination) throws IOException;

    /**
     * Upload.
     * 
     * @param sources the sources
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void upload(final Collection<File> sources, final String destination) throws IOException;

    /**
     * Download.
     * 
     * @param source the source
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void download(String source, File destination) throws IOException;

    /**
     * Execute.
     * 
     * @param command the command
     * @return the process
     * @throws IOException Signals that an I/O exception has occurred.
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
     * Gets the address.
     * 
     * @return the address
     */
    InetAddress getAddress();

    /**
     * Checks if is local.
     * 
     * @return true, if is local
     */
    boolean isLocal();

    /**
     * Shutdown.
     */
    void shutdown();

}
