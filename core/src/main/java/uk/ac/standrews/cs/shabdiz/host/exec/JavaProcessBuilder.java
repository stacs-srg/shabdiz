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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.CompressionUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/**
 * Starts a Java process on a {@link Host} from a class, which contains {@code main} method.
 * The Java classpath is to be specified as if the process is start on the local machine.
 * The classpath may be specified as a combination of {@link File}, {@link URL} or the current JVM classpath.
 * If a given {@link Host} is not local, the classpath files are collected on the local machine and are uploaded to the remote host.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class JavaProcessBuilder implements HostProcessBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaProcessBuilder.class);
    private static final String SPACE = " ";
    private final StringBuffer jvm_arguments;
    private final StringBuffer command_line_arguments;
    private final String main_class;
    private final Set<File> classpath;
    private volatile String java_home;

    /**
     * Constructs a new Java process builder with the given class as the {@code main} class.
     *
     * @param main_class the main class of the Java processes that are started by this process builder
     */
    public JavaProcessBuilder(final Class<?> main_class) {

        this(main_class.getName());
    }

    /**
     * Constructs a new Java process builder with the given fully qualified class name as the {@code main} class.
     *
     * @param main_class_name the fully qualified class name of the Java processes that are started by this process builder
     */
    public JavaProcessBuilder(final String main_class_name) {

        this.main_class = main_class_name;
        jvm_arguments = new StringBuffer();
        command_line_arguments = new StringBuffer();
        classpath = new HashSet<File>();
    }

    @Override
    public Process start(final Host host) throws IOException {

        final String remote_working_directory = prepareRemoteWorkingDirectory(host);
        final String command = assembleRemoteJavaCommand(host);
        LOGGER.debug("preparing to execute command: {}", command);
        return host.execute(remote_working_directory, command);
    }

    private String prepareRemoteWorkingDirectory(final Host host) throws IOException {

        final String remote_working_directory = getRemoteWorkingDirectory(host);
        LOGGER.info("remote working directory: {}", remote_working_directory);
        final File compressed_classpath = File.createTempFile("shabdiz_compressed_cp", ".zip");
        LOGGER.info("compressed classpath: {}", compressed_classpath);
        CompressionUtil.compress(classpath, compressed_classpath);
        host.upload(compressed_classpath, remote_working_directory);
        decompressOnHost(host, remote_working_directory, compressed_classpath);
        return remote_working_directory;
    }

    private void decompressOnHost(final Host host, final String remote_working_directory, final File compressed_classpath) throws IOException {

        //TODO use tar.gz instead of zip if jar is not available; comes with cygwin where as unzip package needs to be installed
        try {
            try {
                ProcessUtil.waitForNormalTerminationAndGetOutput(host.execute(remote_working_directory, "jar xf " + compressed_classpath.getName()));
            } catch (final IOException e) {
                ProcessUtil.waitForNormalTerminationAndGetOutput(host.execute(remote_working_directory, "unzip -q -o " + compressed_classpath.getName()));
            }
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }
    }

    private String getRemoteWorkingDirectory(final Host host) throws IOException {

        return host.getPlatform().getTempDirectory() + "shabdiz_" + UUID.randomUUID().toString();
    }

    private String assembleRemoteJavaCommand(final Host host) throws IOException {

        final StringBuilder command = new StringBuilder();
        final Platform platform = host.getPlatform();
        appendJavaBinPath(command, platform);
        appendJVMArguments(command, platform);
        appendMainClass(command);
        appendCommandLineArguments(command);

        return command.toString();
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

    /** Adds current JVM's classpath to this builder's collection of classpath files. */
    public void addCurrentJVMClasspath() {

        final String classpath = System.getProperty("java.class.path");
        final Set<File> classpath_files = new HashSet<File>();
        for (final String classpath_entry : classpath.split(File.pathSeparator)) {
            classpath_files.add(new File(classpath_entry));
        }
        addClasspath(classpath_files);
    }

    /**
     * Adds the given files to the collection of this builder's classpath files.
     *
     * @param classpath_files the files to be added to this builder's classpath files
     * @return {@code true}, if this builder's classpath files changed as a result of the call
     */
    public boolean addClasspath(final Set<File> classpath_files) {

        return classpath.addAll(classpath_files);
    }

    /**
     * Adds the given file to the collection of classpath files.
     *
     * @param classpath_file the file to be added to the classpath
     * @return {@code true}, if successfully added. {@code False} otherwise.
     */
    public boolean addClasspath(final File classpath_file) {

        return classpath.add(classpath_file);
    }

    /**
     * Attempts to download the given {@code classpath_url}, and adds it to the collection of classpath files.
     *
     * @param classpath_url the URL of file to be added to the classpath
     * @return {@code true}, if successfully added. {@code False} otherwise.
     * @throws IOException if the source URL cannot be opened or an IO error occurs
     */
    public boolean addClasspath(final URL classpath_url) throws IOException {

        final File downloaded_file = File.createTempFile("downloaded_", classpath_url.getFile());
        FileUtils.copyURLToFile(classpath_url, downloaded_file);
        return classpath.add(downloaded_file);
    }

    /**
     * Gets the name of the class that is used as the main class of the process, which are started by this process builder.
     *
     * @return the name of the class that is used as the main class of the process, which are started by this process builder
     */
    public String getMainClassName() {

        return main_class;
    }

    /**
     * Sets the Java home directory on hosts on which Java processes to be started.
     *
     * @param java_home the Java home directory on hosts on which Java processes to be started
     */
    public void setJavaHome(final String java_home) {

        this.java_home = java_home;
    }

    /**
     * Replaces this builder's JVM arguments with the given {@code replacement_arguments}.
     *
     * @param replacement_arguments the arguments to replace this builder's JVM arguments with
     */
    public void replaceJVMArguments(final String replacement_arguments) {

        final String arg = tidyArgument(replacement_arguments);
        jvm_arguments.replace(0, jvm_arguments.length(), arg);
    }

    private String tidyArgument(final String argument) {

        return argument.trim();
    }

    /**
     * Adds the given {@code argument} to this builder's JVM arguments.
     *
     * @param argument the argument to add to the collection of this builder's JVM arguments
     */
    public void addJVMArgument(final String argument) {

        final String arg = tidyArgument(argument);
        jvm_arguments.append(arg).append(SPACE);
    }

    /**
     * Adds the given {@code arguments} to this builder's command line arguments.
     *
     * @param arguments the arguments to add to the collection of this builder's command line arguments
     * @see #addCommandLineArgument(String)
     */
    public void addCommandLineArguments(final Collection<String> arguments) {

        for (final String argument : arguments) {
            addCommandLineArgument(argument);
        }
    }

    /**
     * Adds the given {@code argument} to this builder's command line arguments.
     *
     * @param argument the argument to add to the collection of this builder's command line arguments
     */
    public void addCommandLineArgument(final String argument) {

        //FIXME quote the argument based on platform, in case the argument has special characters
        final String arg = tidyArgument(argument);
        command_line_arguments.append(arg).append(SPACE);
    }
}
