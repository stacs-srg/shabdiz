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
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

public class RemoteJavaProcessBuilder implements RemoteProcessBuilder {

    private static final Logger LOGGER = Logger.getLogger(RemoteJavaProcessBuilder.class.getName());
    private static final String SPACE = " ";
    private static final String TEMP_DIR = "/tmp";
    private final StringBuffer jvm_arguments;
    private final StringBuffer command_line_arguments;
    private final String main_class;
    private final Set<File> classpath;
    private volatile String java_home;

    public RemoteJavaProcessBuilder(final Class<?> main_class) {

        this(main_class.getName());
    }

    public RemoteJavaProcessBuilder(final String main_class) {

        this.main_class = main_class;
        jvm_arguments = new StringBuffer();
        command_line_arguments = new StringBuffer();
        classpath = new HashSet<File>();
    }

    @Override
    public Process start(final Host host) throws IOException {

        final String remote_working_directory = prepareRemoteWorkingDirectory(host);
        final String command = assembleRemoteJavaCommand(host, remote_working_directory);
        LOGGER.info("prepareing to execute command: " + command);
        return host.execute(command);
    }

    private String prepareRemoteWorkingDirectory(final Host host) throws IOException {

        final String remote_working_directory = getRemoteWorkingDirectory(host);
        host.upload(classpath, remote_working_directory);
        return remote_working_directory;
    }

    private String getRemoteWorkingDirectory(final Host host) throws IOException {

        return host.getPlatform().getTempDirectory()   + UUID.randomUUID().toString();
    }

    private String assembleRemoteJavaCommand(final Host host, final String remote_working_directory) throws IOException {

        final StringBuilder command = new StringBuilder();
        final Platform platform = host.getPlatform();
        appendRemoteWorkingDirectory(remote_working_directory, command);
        appendJavaBinPath(command, platform);
        appendJVMArguments(command, platform);
        appendMainClass(command);
        appendCommandLineArguments(command);

        return command.toString();
    }

    private void appendRemoteWorkingDirectory(final String remote_working_directory, final StringBuilder command) {

        command.append("cd");
        command.append(SPACE);
        command.append(remote_working_directory);
        command.append(";");
        command.append(SPACE);
    }

    private void appendCommandLineArguments(final StringBuilder command) {

        command.append(command_line_arguments.toString());
        command.append(SPACE);
    }

    private void appendMainClass(final StringBuilder command) {

        command.append(main_class);
        command.append(SPACE);
    }

    private void appendJVMArguments(final StringBuilder command, final Platform platform) {

        command.append(jvm_arguments.toString());
        appendClasspath(command, platform);
    }

    private void appendClasspath(final StringBuilder command, final Platform platform) {

        command.append("-classpath");
        command.append(SPACE);
                command.append("\"");
        command.append(".");
        command.append(platform.getPathSeparator());
        appendClasspathDirectoryNames(command, platform);
        command.append("*"); // Add all the files with .jar or .JAR extension in the run-time current directory to the classpath
                command.append("\"");
        command.append(SPACE);
    }

    private void appendClasspathDirectoryNames(final StringBuilder command, final Platform platform) {

        final Set<String> classpath_directory_names = new HashSet<String>();
        for (final File classpath_entry : classpath) {
            final String name = classpath_entry.getName();
            if (classpath_entry.isDirectory() && !classpath_directory_names.contains(name)) {
                command.append(name);
                command.append(platform.getPathSeparator());
                classpath_directory_names.add(name);
            }
        }
    }

    private void appendJavaBinPath(final StringBuilder command, final Platform platform) {

        if (java_home != null) {
            command.append(java_home);
            command.append(platform.getSeparator());
            command.append("bin");
            command.append(platform.getSeparator());
        }
        command.append("java");
        command.append(SPACE);
    }

    public boolean addClasspath(final File classpath_file) {

        return classpath.add(classpath_file);
    }

    public boolean addClasspath(final Set<File> classpath_files) {

        return classpath.addAll(classpath_files);
    }

    public boolean addClasspath(final URL classpath_url) throws IOException {

        final File downloaded_file = File.createTempFile("downloaded_", classpath_url.getFile());
        FileUtils.copyURLToFile(classpath_url, downloaded_file);
        return classpath.add(downloaded_file);
    }

    public String getMainClass() {

        return main_class;
    }

    public String getJavaHome() {

        return java_home;
    }

    public void setJavaHome(final String java_home) {

        this.java_home = java_home;
    }

    public void addJVMArgument(final String argument) {

        final String arg = tidyArgument(argument);
        jvm_arguments.append(arg).append(SPACE);
    }

    public void replaceJVMArguments(final String replacement_arguments) {

        final String arg = tidyArgument(replacement_arguments);
        jvm_arguments.replace(0, jvm_arguments.length(), arg);
    }

    public void addJVMArguments(final Collection<String> arguments) {

        for (final String argument : arguments) {
            addJVMArgument(argument);
        }
    }

    private String tidyArgument(final String argument) {

        return argument.trim();
    }

    public void addCommandLineArgument(final String argument) {

        final String arg = tidyArgument(argument);
        command_line_arguments.append(arg).append(SPACE);
    }

    public void addCommandLineArguments(final Collection<String> arguments) {

        for (final String argument : arguments) {
            addCommandLineArgument(argument);
        }
    }

    public void addCurrentJVMClasspath() {

        final String classpath = System.getProperty("java.class.path");
        final Set<File> classpath_files = new HashSet<File>();
        for (final String classpath_entry : classpath.split(File.pathSeparator)) {
            classpath_files.add(new File(classpath_entry));
        }
        addClasspath(classpath_files);
    }
}
