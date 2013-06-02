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
 * The classpath may be specified as a combination of {@link File}, {@link URL} or the current JVM classpath.
 * If a given {@link Host} is not local, the classpath files are collected on the local machine and are uploaded from the local host to the remote host.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class FileBasedJavaProcessBuilder extends JavaProcessBuilder {

    // TODO Implement caching and removal of uploaded library files

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedJavaProcessBuilder.class);
    private final Set<File> classpath;

    /**
     * Constructs a new Java process builder with the given class as the {@code main} class.
     *
     * @param main_class the main class of the Java processes that are started by this process builder
     */
    public FileBasedJavaProcessBuilder(final Class<?> main_class) {

        this(main_class.getName());
    }

    /**
     * Constructs a new Java process builder with the given fully qualified class name as the {@code main} class.
     *
     * @param main_class_name the fully qualified class name of the Java processes that are started by this process builder
     */
    public FileBasedJavaProcessBuilder(final String main_class_name) {

        classpath = new HashSet<File>();
        setMainClass(main_class_name);
    }

    @Override
    public Process start(final Host host, final String... parameters) throws IOException {

        final String remote_working_directory = prepareRemoteWorkingDirectory(host);
        final String command = assembleRemoteJavaCommand(host, parameters);
        LOGGER.debug("preparing to execute command: {}", command);
        return host.execute(remote_working_directory, command);
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

    private String prepareRemoteWorkingDirectory(final Host host) throws IOException {

        final String remote_working_directory = getRemoteWorkingDirectory(host);
        LOGGER.info("remote working directory: {}", remote_working_directory);
        final File compressed_classpath = File.createTempFile("shabdiz_compressed_cp", ".zip");
        LOGGER.info("compressed classpath: {}", compressed_classpath);
        CompressionUtil.toZip(classpath, compressed_classpath);
        host.upload(compressed_classpath, remote_working_directory);
        decompressOnHost(host, remote_working_directory, compressed_classpath);
        return remote_working_directory;
    }

    private void decompressOnHost(final Host host, final String remote_working_directory, final File compressed_classpath) throws IOException {

        //TODO use tar.gz instead of zip if jar is not available; comes with cygwin where as unzip package needs to be installed
        try {

            final String compressed_classpath_file_name = compressed_classpath.getName();
            try {
                ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(remote_working_directory, "jar xf " + compressed_classpath_file_name));
            }
            catch (final IOException e) {
                ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(remote_working_directory, "unzip -q -o " + compressed_classpath_file_name));
            }
        }
        catch (final InterruptedException e) {
            throw new IOException(e);
        }
    }

    private String getRemoteWorkingDirectory(final Host host) throws IOException {

        return host.getPlatform().getTempDirectory() + "shabdiz_" + UUID.randomUUID().toString();
    }

    private String assembleRemoteJavaCommand(final Host host, final String[] parameters) throws IOException {

        final StringBuilder command = new StringBuilder();
        final Platform platform = host.getPlatform();
        appendJavaBinPath(command, platform);
        appendJVMArguments(command);
        appendClasspath(command, platform);
        appendMainClass(command);
        appendCommandLineArguments(command, platform, parameters);

        return command.toString();
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

}
