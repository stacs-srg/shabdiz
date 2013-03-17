/*
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import uk.ac.standrews.cs.shabdiz.api.Platform;
import uk.ac.standrews.cs.shabdiz.platform.LocalPlatform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

/**
 * The Class LocalHost.
 */
public class LocalHost extends AbstractHost {

    private static final Logger LOGGER = Logger.getLogger(LocalHost.class.getName());

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
    public Process execute(final String... commands) throws IOException {

        final List<String> command_list = prepareCommands(commands);
        final ProcessBuilder process_builder = new ProcessBuilder(command_list);
        System.out.println(Arrays.toString(commands));
        return process_builder.start();
    }

    private List<String> prepareCommands(final String... command) {

        final List<String> command_list = new ArrayList<String>(Arrays.asList(command));
        if (Platforms.isUnixBased(getPlatform())) {
            command_list.add(0, "bash");
            command_list.add(1, "-c");
        }
        else {
            command_list.add(0, "cmd.exe");
            command_list.add(1, "/c");
        }
        return command_list;
    }

    private void copy(final File source, final File destination) throws IOException {

        LOGGER.fine("copying: " + source.getName() + "\tto: " + destination.getPath());
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
