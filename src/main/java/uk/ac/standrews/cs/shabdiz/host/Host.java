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
package uk.ac.standrews.cs.shabdiz.host;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import uk.ac.standrews.cs.shabdiz.platform.Platform;

/**
 * Provides hooks to download, upload and execute commands on a host.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Host extends Closeable {

    /**
     * Uploads a given file or directory on the local platform to this host into the given destination.
     * If the given {@code source} is a directory, the files are uploaded recursively.
     * If the given {@code destination} does not exist, an attempt is made to construct any non-existing directories.
     * If the given {@code destination} already exists, the files are overridden.
     * 
     * @param source the source to be uploaded
     * @param destination the path on this host to copy the file(s) into
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void upload(File source, String destination) throws IOException;

    /**
     * Uploads a given collection of files or directories on the local platform to this host into the given destination.
     * If the given {@code source} is a directory, the files are uploaded recursively.
     * If the given {@code destination} does not exist, an attempt is made to construct any non-existing directories.
     * If the given {@code destination} already exists, the files are overridden.
     * 
     * @param sources the sources to be uploaded
     * @param destination the path on this host to copy the file(s) into
     * @throws IOException Signals that an I/O exception has occurred.
     * @see #upload(File, String)
     */
    void upload(final Collection<File> sources, final String destination) throws IOException;

    /**
     * Downloads a given file or directory from this host to the local platform at the given {@code destination}.
     * If the given {@code source} is a directory, the files are downloaded recursively.
     * If the given {@code destination} does not exist, an attempt is made to construct any non-existing directories.
     * If the given {@code destination} already exists, the files are overridden.
     * 
     * @param source the source
     * @param destination the destination
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void download(String source, File destination) throws IOException;

    /**
     * Executes the given command in a new process on this host.
     * 
     * @param command the command
     * @return the process that was started as the result of executing this command
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Process execute(String command) throws IOException;

    /**
     * Executes the given command in a new process on this host at the given working directory.
     * 
     * @param working_directory the working directory
     * @param command the command
     * @return the process that was started as the result of executing this command
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Process execute(String working_directory, String command) throws IOException;

    /**
     * Gets the platform-specific settings of this host.
     * 
     * @return the platform-specific settings of this host
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Platform getPlatform() throws IOException;

    /**
     * Gets the address of this host.
     * 
     * @return the address of this host
     */
    InetAddress getAddress();

    String getName();

    /**
     * Checks if this host represents the local platform.
     * 
     * @return true, if this host represents the local platform
     */
    boolean isLocal();

    /**
     * Closes the streams that are used to manage this host and releases any system resources associated with them.
     * If this host is already closed then invoking this method has no effect.
     * 
     * @throws IOException {@inheritDoc}
     */
    @Override
    void close() throws IOException;
}
