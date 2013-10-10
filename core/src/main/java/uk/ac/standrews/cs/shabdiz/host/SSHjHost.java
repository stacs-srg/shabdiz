package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Scanner;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import net.schmizz.sshj.xfer.scp.SCPUploadClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class SSHjHost extends AbstractHost {

    private static final int SSH_TRANSPORT_TIMEOUT_MILLIS = 10000;
    private static final int SSH_CHANNEL_CONNECTION_TIMEOUT_MILLIS = SSH_TRANSPORT_TIMEOUT_MILLIS;
    private static final int SSH_CONNECTION_TIMEOUT = SSH_TRANSPORT_TIMEOUT_MILLIS;
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHjHost.class);
    private final SSHClient ssh;
    private final Platform platform;

    public SSHjHost(final String host_name, AuthMethod authentication) throws IOException {

        this(host_name, SSHClient.DEFAULT_PORT, authentication);
    }

    public SSHjHost(final String host_name, int ssh_port, AuthMethod authentication) throws IOException {

        super(host_name);
        ssh = new SSHClient();
        configureSSHClient(host_name, ssh_port, authentication);
        platform = Platforms.detectPlatform(this);
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        final SCPFileTransfer scp = ssh.newSCPFileTransfer();
        final SCPUploadClient scp_upload = scp.newSCPUploadClient();
        LOGGER.trace("sending {} to {}", source, destination);
        scp_upload.copy(new FileSystemFile(source), destination);
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        final SCPFileTransfer scp = ssh.newSCPFileTransfer();
        final SCPUploadClient scp_upload = scp.newSCPUploadClient();
        for (File source : sources) {
            LOGGER.trace("sending {} to {}", source, destination);
            scp_upload.copy(new FileSystemFile(source), destination);
        }
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final SCPFileTransfer scp = ssh.newSCPFileTransfer();
        final SCPDownloadClient scp_download = scp.newSCPDownloadClient();
        scp_download.copy(source, new FileSystemFile(destination));
    }

    @Override
    public Process execute(final String command) throws IOException {

        return execute(command, true);
    }

    public Process execute(final String command, final boolean kill_child_processes) throws IOException {

        final Session session = ssh.startSession();
        LOGGER.debug("executing on host {} command: {}", this, command);

        //FIXME determination of ppid is unix specific; generify for windows
        final Session.Command command_exec = session.exec("echo $$;" + command);
        final int ppid = readParentProcessID(command_exec);
        return new Process() {

            @Override
            public OutputStream getOutputStream() {

                return command_exec.getOutputStream();
            }

            @Override
            public InputStream getInputStream() {

                return command_exec.getInputStream();
            }

            @Override
            public InputStream getErrorStream() {

                return command_exec.getErrorStream();
            }

            @Override
            public int waitFor() throws InterruptedException {

                try {
                    command_exec.join();
                }
                catch (ConnectionException e) {
                    throw new InterruptedException("interrupted due to connection failure: " + e.getMessage());
                }
                return exitValue();
            }

            @Override
            public int exitValue() {

                final Integer exit_value = command_exec.getExitStatus();

                if (exit_value != null) { return exit_value; }
                throw new IllegalThreadStateException("remote process has not terminated yet");
            }

            @Override
            public void destroy() {

                try {
                    try {

                        //FIXME kill is Unix specific; generify kill by parent process id
                        if (kill_child_processes) {
                            try {
                                //taken from http://stackoverflow.com/questions/392022/best-way-to-kill-all-child-processes/6481337#6481337
                                final Process kill = execute("CPIDS=$(pgrep -P " + ppid + "); (sleep 2 && kill -KILL $CPIDS &); kill -TERM $CPIDS", false);
                                kill.waitFor();
                                kill.destroy();
                            }
                            catch (IOException e) {
                                LOGGER.error("failed to execute kill_process on host " + getName(), e);
                            }
                            catch (InterruptedException e) {
                                LOGGER.error("interrupted while waiting for kill_process on host " + getName(), e);
                            }
                        }
                        command_exec.close();
                    }
                    finally {
                        session.close();
                    }
                }
                catch (TransportException e) {
                    LOGGER.error("failed to destroy process on host " + getName(), e);
                }
                catch (ConnectionException e) {
                    LOGGER.error("failed to destroy process on host " + getName(), e);
                }
            }
        };
    }

    @Override
    public Process execute(final String working_directory, final String command) throws IOException {

        final String set_working_directory = Commands.CHANGE_DIRECTORY.get(platform, working_directory);
        final String appended_command = Commands.APPENDER.get(platform, set_working_directory, command);
        return execute(appended_command);
    }

    @Override
    public Platform getPlatform() throws IOException {

        return platform;
    }

    @Override
    public void close() throws IOException {

        super.close();
        ssh.disconnect();
    }

    private int readParentProcessID(final Session.Command command_exec) throws IOException {
        final Scanner scanner = new Scanner(command_exec.getInputStream());
        return scanner.nextInt();
    }

    private void configureSSHClient(final String host_name, final int ssh_port, final AuthMethod authentication) throws IOException {

        ssh.loadKnownHosts();
        ssh.connect(host_name, ssh_port);
        ssh.setConnectTimeout(SSH_CONNECTION_TIMEOUT);
        ssh.auth(Platforms.getCurrentUser(), authentication);
        ssh.getTransport().setTimeoutMs(SSH_TRANSPORT_TIMEOUT_MILLIS);
        ssh.getConnection().setTimeoutMs(SSH_CHANNEL_CONNECTION_TIMEOUT_MILLIS);
    }
}
