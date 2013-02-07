package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.barreleye.ChannelSftp;
import uk.ac.standrews.cs.barreleye.ChannelType;
import uk.ac.standrews.cs.barreleye.ChannelSftp.LsEntry;
import uk.ac.standrews.cs.barreleye.exception.SFTPException;


public class RemoteJavaProcessBuilder implements RemoteProcessBuilder {

    private static final Logger LOGGER = Logger.getLogger(RemoteJavaProcessBuilder.class.getName());
    private static final String SPACE = " ";
    private static final String TEMP_DIR = "/tmp";
    private final StringBuilder jvm_arguments;
    private final StringBuilder command_line_arguments;
    private final String main_class;
    private final Set<File> classpath;
    private volatile String java_home;

    public RemoteJavaProcessBuilder(final Class<?> main_class) {

        this(main_class.getName());
    }

    public RemoteJavaProcessBuilder(final String main_class) {

        this.main_class = main_class;
        jvm_arguments = new StringBuilder();
        command_line_arguments = new StringBuilder();
        classpath = new HashSet<File>();
    }

    @Override
    public Process start(final Host host) throws IOException, InterruptedException {

        final String remote_working_directory = prepareRemoteWorkingDirectory(host);
        final String command = assembleRemoteJavaCommand(host, remote_working_directory);
        LOGGER.info("prepareing to execute command: " + command);
        final RemoteCommandBuilder command_builder = new RemoteCommandBuilder(command);
        return command_builder.start(host);
    }

    private String prepareRemoteWorkingDirectory(final Host host) throws IOException {

        final ChannelSftp sftp = host.openSSHChannel(ChannelType.SFTP);
        try {
            sftp.connect();
            final String remote_working_directory = createRemoteWorkingDirectory(sftp);
            LOGGER.log(Level.INFO, "created remote directory: " + remote_working_directory);
            transferFilesRecursively(sftp, classpath);
            return remote_working_directory;
        }
        finally {
            sftp.disconnect();
        }
    }

    private void transferFilesRecursively(final ChannelSftp sftp, final Collection<File> files) throws SFTPException {

        for (final File file : files) {
            if (file.isDirectory()) {
                transferDirectory(sftp, file);
            }
            else {
                transferFile(sftp, file);
            }
        }
    }

    private void transferFile(final ChannelSftp sftp, final File file) throws SFTPException {

        final String file_name = file.getName();
        if (!exists(file_name, sftp)) {
            sftp.put(file.getAbsolutePath(), file_name);
        }
    }

    private void transferDirectory(final ChannelSftp sftp, final File path) throws SFTPException {

        assert path.isDirectory();
        final String directory_name = path.getName();
        System.out.println(path);
        if (!exists(directory_name, sftp)) {
            sftp.mkdir(directory_name);
        }
        sftp.cd(directory_name);
        transferFilesRecursively(sftp, Arrays.asList(path.listFiles()));
        sftp.cd("../");
    }

    private boolean exists(final String name, final ChannelSftp sftp) throws SFTPException {

        final List<LsEntry> list = sftp.ls(".");
        for (final LsEntry entry : list) {
            if (entry.getFilename().equals(name)) { return true; }
        }
        return false;
    }

    private String createRemoteWorkingDirectory(final ChannelSftp sftp) throws SFTPException {

        final String working_directory = UUID.randomUUID().toString();
        sftp.cd(TEMP_DIR); //TODO check if /tmp exist
        sftp.mkdir(working_directory);
        sftp.cd(working_directory);
        return sftp.pwd();
    }

    private String assembleRemoteJavaCommand(final Host host, final String remote_working_directory) throws IOException, InterruptedException {

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

        command.append("-cp");
        command.append(SPACE);
        command.append(".");
        command.append(platform.getPathSeparator());
        appendClasspathDirectoryNames(command, platform);
        command.append("*");
        command.append(SPACE);
    }

    private void appendClasspathDirectoryNames(final StringBuilder command, final Platform platform) {

        final Set<String> classpath_directory_names = new HashSet<String>();
        for (final File classpath_entry : classpath) {
            final String name = classpath_entry.getName();
            if (classpath_entry.isDirectory() && !classpath_directory_names.contains(name)) {
                command.append(classpath_entry.getName());
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

    private String tidyArgument(final String argument) {

        return argument.trim();
    }

    public void addCommandLineArgument(final String argument) {

        final String arg = tidyArgument(argument);
        command_line_arguments.append(arg).append(SPACE);
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
