package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import uk.ac.standrews.cs.barreleye.ChannelExec;
import uk.ac.standrews.cs.barreleye.ChannelSftp;
import uk.ac.standrews.cs.barreleye.ChannelSftp.LsEntry;
import uk.ac.standrews.cs.barreleye.ChannelType;
import uk.ac.standrews.cs.barreleye.SSHChannel;
import uk.ac.standrews.cs.barreleye.SSHClient;
import uk.ac.standrews.cs.barreleye.SSHClientFactory;
import uk.ac.standrews.cs.barreleye.SftpATTRS;
import uk.ac.standrews.cs.barreleye.exception.SFTPException;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.active.Credentials;
import uk.ac.standrews.cs.shabdiz.active.PublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.util.PlatformUtil;

public class RemoteSSHHost extends Host {

    private static final Logger LOGGER = Logger.getLogger(RemoteSSHHost.class.getName());
    private static final int SSH_CONNECTION_TIMEOUT = (int) new Duration(15, TimeUnit.SECONDS).getLength(TimeUnit.MILLISECONDS);
    private static final int DEFAULT_SSH_PORT = 22;
    private final SSHClient ssh;
    private final ReentrantLock platform_lock;
    private volatile Platform platform;

    public RemoteSSHHost(final String name, final Credentials credentials) throws IOException {

        super(name, credentials);
        ssh= createSSHClient(name, credentials);
        platform_lock = new ReentrantLock();
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        upload(Arrays.asList(source), destination);
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        final ChannelSftp sftp = openSSHChannel(ChannelType.SFTP);
        try {
            sftp.connect(SSH_CONNECTION_TIMEOUT);
            prepareRemoteDestination(destination, sftp);
            uploadRecursively(sftp, sources);
        }
        finally {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
    }

    private void prepareRemoteDestination(final String destination, final ChannelSftp sftp) throws IOException {

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

    private void uploadRecursively(final ChannelSftp sftp, final Collection<File> files) throws IOException {

        for (final File file : files) {
            LOGGER.info("Uploading: " + file.getAbsolutePath());
            if (file.isDirectory()) {
                uploadDirectoryRecursively(sftp, file);
            }
            else {
                uploadFile(sftp, file);
            }
        }
    }

    private void uploadFile(final ChannelSftp sftp, final File file) throws IOException {

        final String file_name = file.getName();

        if (!exists(file_name, sftp)) {
            sftp.put(file.getAbsolutePath(), file_name);
        }
    }

    private void uploadDirectoryRecursively(final ChannelSftp sftp, final File path) throws IOException {

        assert path.isDirectory();
        final String directory_name = path.getName();
        if (!exists(directory_name, sftp)) {
            sftp.mkdir(directory_name);
        }
        sftp.cd(directory_name);
        uploadRecursively(sftp, Arrays.asList(path.listFiles()));
        sftp.cd("../");
    }

    private boolean exists(final String name, final ChannelSftp sftp) throws SFTPException {

        final List<LsEntry> list = sftp.ls(".");
        for (final LsEntry entry : list) {
            if (entry.getFilename().equals(name)) { return true; }
        }
        return false;
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final ChannelSftp sftp = openSSHChannel(ChannelType.SFTP);
        try {
            sftp.connect();
            prepareLocalDestination(destination);
            final SftpATTRS stat = sftp.stat(source);
            downloadRecursively(sftp, source, destination, stat);
        }
        finally {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
    }

    private void downloadRecursively(final ChannelSftp sftp, final String source, final File destination, final SftpATTRS stat) throws SFTPException {

        if (stat.isDir()) {
            downloadDirectory(sftp, source, destination);
        }
        else {
            downloadFile(sftp, source, destination);
        }

    }

    private void downloadDirectory(final ChannelSftp sftp, final String source, final File destination) throws SFTPException {

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

    private void downloadFile(final ChannelSftp sftp, final String source, final File destination) throws SFTPException {

        sftp.get(source, destination.getAbsolutePath());
    }

    private void prepareLocalDestination(final File destination) {

        destination.mkdirs();
    }

    @Override
    public Process execute(final String... command) throws IOException {

        final StringBuilder sb = new StringBuilder();
        for (final String cmd : command) {
            sb.append(cmd);
            sb.append(" ");
        }
        return new ChannelExecProcess(sb.toString());
    }

    @Override
    public Platform getPlatform() throws IOException {

        platform_lock.lock();
        try {
            if (platform == null) {
                platform = PlatformUtil.detectRemotePlatformUsingUname(this);
            }

            return platform;
        }
        finally {
            platform_lock.unlock();
        }
    }


    private static SSHClient createSSHClient(final String host_name, final Credentials credentials) throws IOException {

        final SSHClientFactory session_factory = SSHClientFactory.getInstance();
        final SSHClient session = session_factory.createSession(credentials.getUsername(), host_name, DEFAULT_SSH_PORT);
        PublicKeyCredentials.setSSHKnownHosts(session_factory);
        return session;
    }

    public <T extends SSHChannel> T openSSHChannel(final ChannelType type) throws IOException {

        synchronized (ssh) {
            if (!ssh.isConnected()) {
                initialiseSession();
            }
        }
        return ssh.openChannel(type);
    }

    private void initialiseSession() throws IOException {

        if (credentials != null) {
            credentials.authenticate(ssh);
        }
        ssh.connect(SSH_CONNECTION_TIMEOUT);
    }

    private class ChannelExecProcess extends Process {

        private final PipedOutputStream out;
        private final PipedInputStream in;
        private final PipedInputStream err;
        private final ChannelExec channel;
        private final String command;

        public ChannelExecProcess(final String command) throws IOException {

            this.command = command;
            this.channel = openSSHChannel(ChannelType.EXEC);
            in = new PipedInputStream();
            err = new PipedInputStream();
            out = new PipedOutputStream();
            configureChannel();
        }

        private void configureChannel() throws IOException {

            channel.setOutputStream(new PipedOutputStream(in));
            channel.setErrStream(new PipedOutputStream(err));
            channel.setInputStream(new PipedInputStream(out));
            channel.setCommand(command);
            channel.connect(SSH_CONNECTION_TIMEOUT);
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

            channel.awaitEOF();
            channel.disconnect();
            return exitValue();
        }

        @Override
        public int exitValue() {

            return channel.getExitStatus();
        }

        @Override
        public void destroy() {

            /*
             * Would have been nice to send KILL signal via SSH Channel.
             * But unfortunately it is not implemented in OpenSSH:
             * https://bugzilla.mindrot.org/show_bug.cgi?id=1424
             */

            attemptSelfTermination();
            closePipedStreams();
            channel.disconnect();
        }

        private void attemptSelfTermination() {

            try {
                //FIXME if the incomming connection is blocked by firewall this call blocks for ever.
                //TODO add timeout
                kill();
            }
            catch (final IOException e) {
                LOGGER.log(Level.SEVERE, "unable to kill process", e);
            }
            catch (final InterruptedException e) {
                LOGGER.log(Level.SEVERE, "unable to kill process", e);
            }
        }

        private void closePipedStreams() {

            try {
                in.close();
                out.close();
                err.close();
            }
            catch (final IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }

        private void kill() throws IOException, InterruptedException {

            final String kill_command = assembleKillCommand();
            LOGGER.info(kill_command);
            final ChannelExec kill_channel = ssh.openChannel(ChannelType.EXEC);
            try {
                kill_channel.setCommand(kill_command);
                kill_channel.connect();
                kill_channel.awaitEOF();
            }
            finally {
                channel.disconnect();
            }
        }

        private String assembleKillCommand() {

            //ps -o pid,command -ax -u <USER> | grep '\Q<COMMAND>\E' | sed 's/^[a-z]*[ ]*\([0-9]*\).*/\1/' | tr '\n' ',' | sed 's/,$//g' | xargs pkill -TERM -P $1
            //TODO figure out what's wrong with pkill -f
            final StringBuilder kill_command = new StringBuilder();
            appendListProcessesByUser(kill_command);
            appendPipe(kill_command);
            appendSearchByCommand(kill_command);
            appendPipe(kill_command);
            appendListMatchingProcessIDsSeparatedByComma(kill_command);
            appendPipe(kill_command);
            appendKillProcessAndSubprocesses(kill_command);
            return kill_command.toString();
        }

        private void appendKillProcessAndSubprocesses(final StringBuilder kill_command) {

            kill_command.append("xargs pkill -TERM -P $1;");
        }

        private void appendPipe(final StringBuilder kill_command) {

            kill_command.append(" | ");
        }

        private void appendListMatchingProcessIDsSeparatedByComma(final StringBuilder kill_command) {

            kill_command.append("sed 's/^[a-z]*[ ]*\\([0-9]*\\).*/\\1/'"); // List process IDs, one per line
            appendPipe(kill_command);
            kill_command.append("tr '\\n' ','"); // Replace '\n' with comma
            appendPipe(kill_command);
            kill_command.append("sed 's/,$//g'"); // Remove the tailing comma

        }

        private void appendSearchByCommand(final StringBuilder kill_command) {

            kill_command.append("grep '");
            kill_command.append(Pattern.quote(command));
            kill_command.append("'");
        }

        private void appendListProcessesByUser(final StringBuilder kill_command) {

            kill_command.append("ps -o pid,command -ax -u ");
            kill_command.append(channel.getSession().getUserName());
        }
    }

}
