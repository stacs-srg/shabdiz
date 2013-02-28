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
package uk.ac.standrews.cs.shabdiz;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import uk.ac.standrews.cs.shabdiz.new_api.Platform;
import uk.ac.standrews.cs.shabdiz.platform.LocalPlatform;
import uk.ac.standrews.cs.shabdiz.util.PlatformUtil;

/**
 * The Class LocalHost.
 */
public class LocalHost extends AbstractHost {

    /**
     * Instantiates a new local host.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public LocalHost() throws IOException {

        super(InetAddress.getLocalHost(), null);
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
        return process_builder.start();
    }

    private List<String> prepareCommands(final String... command) {

        final List<String> command_list = new ArrayList<String>(Arrays.asList(command));
        if (PlatformUtil.isUnixBased(getPlatform())) {
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

        System.out.println("copying: " + source.getName() + "\tto: " + destination.getPath());
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
