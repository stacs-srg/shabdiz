package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.credentials.SSHCredential;
import uk.ac.standrews.cs.shabdiz.credentials.SSHPasswordCredential;
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

public class JSchSSHost extends AbstractHost {

    private static final Logger LOGGER = Logger.getLogger(JSchSSHost.class.getName());
    private static final int SSH_CONNECTION_TIMEOUT = (int) new Duration(15, TimeUnit.SECONDS).getLength(TimeUnit.MILLISECONDS);
    private static final int DEFAULT_SSH_PORT = 22;

    private final JSch ssh_client;
    private final ReentrantLock platform_lock;
    private volatile Platform platform;
    private final SSHCredential credential;
    private final String username;

    public JSchSSHost(final String host_name, final SSHCredential credential) throws IOException, JSchException {

        super(host_name);
        this.credential = credential;
        username = credential.getUsername();
        platform_lock = new ReentrantLock();
        ssh_client = new JSch();
        ssh_client.setKnownHosts(credential.getKnownHostsFile());
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
        }
        catch (final SftpException e) {
            throw new IOException(e);
        }
        finally {
            disconnect(session, sftp);
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
            LOGGER.fine("Uploading: " + file.getAbsolutePath());
            if (file.isDirectory()) {
                uploadDirectoryRecursively(sftp, file);
            }
            else {
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

        final List<LsEntry> list = sftp.ls(".");
        for (final LsEntry entry : list) {
            if (entry.getFilename().equals(name)) { return true; }
        }
        return false;
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final Session session = createSession();
        final ChannelSftp sftp = openSftpChannel(session);
        prepareLocalDestination(destination);
        try {
            final SftpATTRS stat = sftp.stat(source);
            downloadRecursively(sftp, source, destination, stat);
        }
        catch (final SftpException e) {
            throw new IOException(e);
        }
        finally {
            disconnect(session, sftp);
        }
    }

    private Session createSession() throws IOException {

        try {
            final Session session = ssh_client.getSession(username, getName(), DEFAULT_SSH_PORT);
            session.setPassword(new String(((SSHPasswordCredential) credential).getPassword()));
            session.connect(SSH_CONNECTION_TIMEOUT);
            return session;
        }
        catch (final JSchException e) {
            throw new IOException(e);
        }
    }

    private ChannelSftp openSftpChannel(final Session session) throws IOException {

        try {
            final ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(SSH_CONNECTION_TIMEOUT);
            return sftp;
        }
        catch (final JSchException e) {
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
        }
        else {
            downloadFile(sftp, source, destination);
        }

    }

    private void downloadDirectory(final ChannelSftp sftp, final String source, final File destination) throws SftpException {

        final String directory_name = FilenameUtils.getName(source);
        final File sub_destination = new File(destination, directory_name);
        sub_destination.mkdirs();
        sftp.cd(source);
        final List<LsEntry> ls_entries = sftp.ls(".");
        for (final LsEntry ls_entry : ls_entries) {
            final String remote_file_name = ls_entry.getFilename();
            if (!remote_file_name.equals(".") && !remote_file_name.equals("..")) {
                downloadRecursively(sftp, remote_file_name, sub_destination, ls_entry.getAttrs());
            }
        }
        sftp.cd("../");
    }

    private void downloadFile(final ChannelSftp sftp, final String source, final File destination) throws SftpException {

        sftp.get(source, destination.getAbsolutePath());
    }

    private void prepareLocalDestination(final File destination) {

        destination.mkdirs();
    }

    @Override
    public Process execute(final String command) throws IOException {

        return new SSHManagedRemoteProcess(command);
    }

    @Override
    public Process execute(final String working_directory, final String command) throws IOException {

        LOGGER.info(working_directory);
        LOGGER.info(command);

        final StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(working_directory);
        sb.append(";");
        sb.append(" ");
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
        }
        finally {
            platform_lock.unlock();
        }
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
                channel.connect(SSH_CONNECTION_TIMEOUT);
            }
            catch (final JSchException e) {
                throw new IOException(e);
            }
            return channel;
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

        private ChannelExec openExecChannel() throws IOException {

            ChannelExec channel;
            try {
                channel = (ChannelExec) session.openChannel("exec");
            }
            catch (final JSchException e) {
                throw new IOException(e);
            }
            return channel;
        }

        @Override
        public int waitFor() throws InterruptedException {

            termination_latch.await();
            return exitValue();
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
        public int exitValue() {

            return channel.getExitStatus();
        }

        @Override
        public void destroy() {

            try {
                in.close();
                out.close();
                err.close();
            }
            catch (final IOException e) {
                LOGGER.log(Level.FINE, "failed to close IO streams", e);
            }
            finally {
                disconnect(session, channel);
            }
        }
    }
}
