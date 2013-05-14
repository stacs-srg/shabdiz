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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.staticiser.jetson.util.CloseableUtil;

/**
 * Implements a {@link Host} that uses SSH2 to upload, download and execute commands.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class SSHHost extends AbstractHost {

    /** The Constant DEFAULT_SSH_PORT. */
    public static final int DEFAULT_SSH_PORT = 22;
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHHost.class);
    private static final int DEFAULT_SSH_CONNECTION_TIMEOUT_IN_MILLIS = (int) TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private final JSch ssh_session_factory;
    private final ReentrantLock platform_lock;
    private final String username;
    private final transient SSHCredentials credentials;
    private volatile Platform platform;
    private volatile int ssh_port;
    private volatile int ssh_connection_timeout_in_millis;

    /**
     * Instantiates a new SSH-managed host.
     *
     * @param host_name the host name
     * @param credentials the credentials
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SSHHost(final String host_name, final SSHCredentials credentials) throws IOException {

        this(InetAddress.getByName(host_name), credentials);
    }

    /**
     * Instantiates a new SSH-managed host.
     *
     * @param host_address the host address
     * @param credentials the credentials
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SSHHost(final InetAddress host_address, final SSHCredentials credentials) throws IOException {

        super(host_address);
        this.credentials = credentials;
        username = credentials.getUsername();
        platform_lock = new ReentrantLock();
        ssh_session_factory = new JSch();
        configure();
    }

    private void configure() throws IOException {

        try {
            ssh_session_factory.setKnownHosts(credentials.getKnownHostsFile());
        } catch (final JSchException e) {
            throw new IOException(e);
        }
        ssh_port = DEFAULT_SSH_PORT;
        ssh_connection_timeout_in_millis = DEFAULT_SSH_CONNECTION_TIMEOUT_IN_MILLIS;
    }

    /**
     * Sets the port number that is used for SSH connection. The default port is set to {@value #DEFAULT_SSH_PORT}.
     *
     * @param ssh_port the new sSH port
     */
    public void setSSHPort(final int ssh_port) {

        this.ssh_port = ssh_port;
    }

    /**
     * Sets the SSH connection timeout.
     *
     * @param timeout the timeout
     * @param unit the unit of the specified timeout
     */
    public void setSSHConnectionTimeout(final long timeout, final TimeUnit unit) {

        final long timeout_in_millis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        //cast to integer to cope with the JSch's mad bad API
        this.ssh_connection_timeout_in_millis = (int) timeout_in_millis;
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        upload(Arrays.asList(source), destination);
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        final Session session = createSession();
        final ChannelSftp sftp = openSftpChannel(session);
        try {
            prepareRemoteDestination(destination, sftp);
            uploadRecursively(sftp, sources);
        } catch (final SftpException e) {
            throw new IOException(e);
        } finally {
            disconnect(session, sftp);
        }
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final Session session = createSession();
        final ChannelSftp sftp = openSftpChannel(session);
        prepareLocalDestination(destination);
        try {
            final SftpATTRS stat = sftp.stat(source);
            downloadRecursively(sftp, source, destination, stat);
        } catch (final SftpException e) {
            throw new IOException(e);
        } finally {
            disconnect(session, sftp);
        }
    }

    @Override
    public Process execute(final String command) throws IOException {

        LOGGER.info("executing {}", command);
        return new SSHManagedRemoteProcess(command);
    }

    @Override
    public Process execute(final String working_directory, final String command) throws IOException {

        if (working_directory == null) {
            return execute(command);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(working_directory);
        sb.append("; ");
        sb.append(command);
        return execute(sb.toString());
    }

    @Override
    public Platform getPlatform() throws IOException {

        platform_lock.lock();
        try {
            if (platform == null) {
                platform = Platforms.detectPlatform(this);
            }

            return platform;
        } finally {
            platform_lock.unlock();
        }
    }

    private void prepareRemoteDestination(final String destination, final ChannelSftp sftp) throws SftpException, IOException {

        final String separator = String.valueOf(getPlatform().getSeparator());
        final String[] directory_names = destination.split(separator);
        final boolean absolute = destination.startsWith(separator);
        if (absolute) {
            sftp.cd(separator);
        }

        for (final String directory_name : directory_names) {
            if (directory_name.length() > 0) {
                if (!exists(directory_name, sftp)) {
                    sftp.mkdir(directory_name);
                }
                sftp.cd(directory_name);
            }
        }
    }

    private void uploadRecursively(final ChannelSftp sftp, final Collection<File> files) throws SftpException {

        for (final File file : files) {
            LOGGER.debug("Uploading {}", file.getAbsolutePath());
            if (file.isDirectory()) {
                uploadDirectoryRecursively(sftp, file);
            } else {
                uploadFile(sftp, file);
            }
        }
    }

    private void uploadFile(final ChannelSftp sftp, final File file) throws SftpException {

        final String file_name = file.getName();

        if (!exists(file_name, sftp)) {
            sftp.put(file.getAbsolutePath(), file_name);
        }
    }

    private void uploadDirectoryRecursively(final ChannelSftp sftp, final File path) throws SftpException {

        assert path.isDirectory();
        final String directory_name = path.getName();
        if (!exists(directory_name, sftp)) {
            sftp.mkdir(directory_name);
        }
        sftp.cd(directory_name);
        uploadRecursively(sftp, Arrays.asList(path.listFiles()));
        sftp.cd("../");
    }

    private boolean exists(final String name, final ChannelSftp sftp) throws SftpException {

        @SuppressWarnings("unchecked")
        final List<LsEntry> list = sftp.ls(".");
        for (final LsEntry entry : list) {
            if (entry.getFilename().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Session createSession() throws IOException {

        try {
            final Session session = ssh_session_factory.getSession(username, getName(), ssh_port);
            credentials.authenticate(ssh_session_factory, session);
            session.connect(ssh_connection_timeout_in_millis);
            return session;
        } catch (final JSchException e) {
            throw new IOException(e);
        }
    }

    private ChannelSftp openSftpChannel(final Session session) throws IOException {

        try {
            final ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(DEFAULT_SSH_CONNECTION_TIMEOUT_IN_MILLIS);
            return sftp;
        } catch (final JSchException e) {
            throw new IOException(e);
        }
    }

    private void disconnect(final Session session, final Channel channel) {

        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private void downloadRecursively(final ChannelSftp sftp, final String source, final File destination, final SftpATTRS stat) throws SftpException {

        if (stat.isDir()) {
            downloadDirectory(sftp, source, destination);
        } else {
            downloadFile(sftp, source, destination);
        }

    }

    private void downloadDirectory(final ChannelSftp sftp, final String source, final File destination) throws SftpException {

        final String directory_name = FilenameUtils.getName(source);
        final File sub_destination = new File(destination, directory_name);
        makeDirectoriesIfNeeded(sub_destination);

        sftp.cd(source);
        @SuppressWarnings("unchecked")
        final List<LsEntry> ls_entries = sftp.ls(".");
        for (final LsEntry ls_entry : ls_entries) {
            final String remote_file_name = ls_entry.getFilename();
            if (!remote_file_name.equals(".") && !remote_file_name.equals("..")) {
                downloadRecursively(sftp, remote_file_name, sub_destination, ls_entry.getAttrs());
            }
        }
        sftp.cd("../");
    }

    private void makeDirectoriesIfNeeded(final File sub_destination) {
        if (!sub_destination.exists() || !sub_destination.isDirectory()) {
            final boolean created_sub_destination = sub_destination.mkdirs();
            if (!created_sub_destination) {
                LOGGER.warn("did not create directories {}", sub_destination);
            }
        }
    }

    private void downloadFile(final ChannelSftp sftp, final String source, final File destination) throws SftpException {

        sftp.get(source, destination.getAbsolutePath());
    }

    private void prepareLocalDestination(final File destination) {

        makeDirectoriesIfNeeded(destination);
    }

    private final class SSHManagedRemoteProcess extends Process {

        private final ChannelExec channel;
        private final CountDownLatch termination_latch;
        private final InputStream in;
        private final OutputStream out;
        private final InputStream err;
        private final Session session;
        private final String command;

        private SSHManagedRemoteProcess(final String command) throws IOException {

            this.command = command;
            this.session = createSession();
            this.termination_latch = new CountDownLatch(1);

            channel = createChannel();
            in = channel.getInputStream();
            err = channel.getErrStream();
            out = channel.getOutputStream();
        }

        private ChannelExec createChannel() throws IOException {

            final ChannelExec channel = openExecChannel();
            final PipedInputStream termination_aware_in = createTerminationAwareInputStreamWrapper();
            channel.setInputStream(termination_aware_in);
            channel.setCommand(command);
            try {
                channel.connect(DEFAULT_SSH_CONNECTION_TIMEOUT_IN_MILLIS);
            } catch (final JSchException e) {
                throw new IOException(e);
            }
            return channel;
        }

        private ChannelExec openExecChannel() throws IOException {

            try {
                return (ChannelExec) session.openChannel("exec");
            } catch (final JSchException e) {
                throw new IOException(e);
            }
        }

        private PipedInputStream createTerminationAwareInputStreamWrapper() {

            return new PipedInputStream() {

                @Override
                public void close() throws IOException {

                    termination_latch.countDown();
                    super.close();
                }
            };
        }

        @Override
        public OutputStream getOutputStream() {

            return out;
        }

        @Override
        public InputStream getInputStream() {

            return in;
        }

        @Override
        public InputStream getErrorStream() {

            return err;
        }

        @Override
        public int waitFor() throws InterruptedException {

            termination_latch.await();
            return exitValue();
        }

        @Override
        public int exitValue() {

            return channel.getExitStatus();
        }

        @Override
        public void destroy() {

            CloseableUtil.closeQuietly(in, out, err);
            disconnect(session, channel);
        }
    }
}
