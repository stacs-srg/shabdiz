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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.platform.LocalPlatform;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

/**
 * The Class LocalHost.
 */
public class LocalHost extends AbstractHost {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalHost.class);

    /**
     * Instantiates a new local host.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public LocalHost() throws IOException {

        super(InetAddress.getLocalHost());
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        final File destination_file = new File(destination);
        copy(source, destination_file);
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final File source_file = new File(source);
        copy(source_file, destination);
    }

    @Override
    public Process execute(final String command) throws IOException {

        return execute(".", command);
    }

    @Override
    public Process execute(final String working_directory, final String command) throws IOException {

        final ProcessBuilder process_builder = createProcessBuilder(command);
        process_builder.directory(new File(working_directory));
        LOGGER.info("platform: {}, working dir: {}, command: {}", getPlatform(), working_directory, process_builder.command());
        return process_builder.start();
    }

    private ProcessBuilder createProcessBuilder(final String command) {

        return Platforms.isUnixBased(getPlatform()) ? new ProcessBuilder("bash", "-c", command) : new ProcessBuilder("cmd.exe", "/c", command);
    }

    private void copy(final File source, final File destination) throws IOException {

        LOGGER.debug("copying: {}, to {}", source.getName(), destination.getPath());
        if (source.isFile()) {
            FileUtils.copyFile(source, new File(destination, source.getName()));
        }
        else {
            FileUtils.copyDirectory(source, destination);
        }
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        for (final File source : sources) {
            upload(source, destination);
        }
    }

    @Override
    public Platform getPlatform() {

        return LocalPlatform.getInstance();
    }
}
