package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class LocalHost extends Host {

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
        if (File.separatorChar == Platform.UNIX.getSeparator()) {
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
    public Platform getPlatform() throws IOException {

        return Platform.LOCAL;
    }
}
