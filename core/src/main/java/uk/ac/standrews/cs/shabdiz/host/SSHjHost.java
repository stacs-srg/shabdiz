package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;
import uk.ac.standrews.cs.shabdiz.platform.SimplePlatform;

import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.readLine;

/**
 * Presents a {@link Host} that uses SSH2 to upload, download and execute commands.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) 
 */
public class SSHjHost extends AbstractHost {

    private static final PromiscuousVerifier PROMISCUOUS_HOST_VERIFIER = new PromiscuousVerifier();
    private static final int SSH_TRANSPORT_TIMEOUT_MILLIS = 15000;
    private static final int SSH_CHANNEL_CONNECTION_TIMEOUT_MILLIS = SSH_TRANSPORT_TIMEOUT_MILLIS;
    private static final int SSH_CONNECTION_TIMEOUT = SSH_TRANSPORT_TIMEOUT_MILLIS;
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHjHost.class);
    private final SSHClient ssh;
    private final Platform platform;
    private boolean destroy_process_forcefully;

    /**
     * Instantiates a new SSH-managed host.
     *
     * @param host_name the host name
     * @param authentication the authentication method
     * @throws IOException if failure occurs while establishing SSH connection
     */
    public SSHjHost(final String host_name, final AuthMethod authentication) throws IOException {

        this(host_name, SSHClient.DEFAULT_PORT, authentication);
    }

    /**
     * Instantiates a new SSH-managed host.
     *
     * @param host_name the host name
     * @param ssh_port the ssh port on this host
     * @param authentication the authentication method
     * @throws IOException if failure occurs while establishing SSH connection
     */
    public SSHjHost(final String host_name, final int ssh_port, final AuthMethod authentication) throws IOException {

        super(host_name);
        ssh = new SSHClient();
        configureSSHClient(host_name, ssh_port, authentication);
        platform = Platforms.detectPlatform(this);
    }

    /**
     * Instantiates a new SSH-managed host. Skips automatic platform detection and uses the given platform as the platform for this host.
     *
     * @param host_name the host name
     * @param ssh_port the ssh port on this host
     * @param authentication the authentication method
     * @param platform the platform of this host
     * @throws IOException if failure occurs while establishing SSH connection
     */
    public SSHjHost(final String host_name, final int ssh_port, final AuthMethod authentication, final Platform platform) throws IOException {

        super(host_name);
        this.platform = platform;
        ssh = new SSHClient();
        configureSSHClient(host_name, ssh_port, authentication);
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        final String destination_path = SimplePlatform.addTailingSeparator(platform.getSeparator(), destination);
        final SFTPClient sftp = ssh.newSFTPClient();
        LOGGER.debug("Uploading {} to {} on host {} ", source, destination, getName());
        upload(sftp, source, destination_path);
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        final String destination_path = SimplePlatform.addTailingSeparator(platform.getSeparator(), destination);
        final SFTPClient sftp = ssh.newSFTPClient();
        for (final File source : sources) {
            LOGGER.debug("Uploading {} to {} on host {} ", source, destination, getName());
            upload(sftp, source, destination_path);
        }
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        final SFTPClient sftp = ssh.newSFTPClient();
        LOGGER.debug("downloading {} from host {} to {}", source, getName(), destination);
        sftp.get(source, new FileSystemFile(destination));
    }

    @Override
    public Process execute(final String command) throws IOException {

        return execute(command, true);
    }

    /**
     * Executes the given {@code command} on this host and returns a {@link Process} corresponding to the executed command. 
     * This method allows to specify whether to kill the whole process tree when the returned process is {@link Process#destroy() destroyed}.
     *
     * @param command the command to execute on this host
     * @param kill_process_tree whether to kill the process tree of the returned process at the time of destruction
     * @return the spawned process on this host
     * @throws IOException if a communication failure occurs 
     */
    public Process execute(final String command, final boolean kill_process_tree) throws IOException {

        final Session session = ssh.startSession();
        LOGGER.debug("executing on host {} command: {}", this, command);

        //FIXME determination of ppid is unix specific; generify for windows
        final Session.Command command_exec = session.exec("echo $$;" + command);
        final int parent_pid = readParentProcessID(command_exec);
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

                        if (kill_process_tree) {
                            killProcessTree();
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

            private void killProcessTree() {

                try {
                    final Process kill = execute(getKillCommand(), false);
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

            private String getKillCommand() {

                return "CPIDS=$(pgrep -P " + parent_pid + "); " + (!destroy_process_forcefully ? "(sleep 0.5 && kill -KILL $CPIDS &); kill -TERM $CPIDS" : "kill -KILL $CPIDS");
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

    /**
     * Gets whether to destroy spawned processes forcefully.
     *
     * @return whether to destroy spawned processes forcefully
     */
    public boolean isDestroyProcessForcefully() {

        return destroy_process_forcefully;
    }

    /**
     * Sets whether to destroy spawned processes forcefully.
     *
     * @param destroy_process_forcefully whether to destroy spawned processes forcefully
     */
    public void setDestroyProcessForcefully(final boolean destroy_process_forcefully) {

        this.destroy_process_forcefully = destroy_process_forcefully;
    }

    private void upload(final SFTPClient sftp, final File file, final String destination) throws IOException {

        final String file_name = file.getName();
        sftp.put(new FileSystemFile(file), destination + file_name);
    }

    private int readParentProcessID(final Session.Command command_exec) throws IOException {

        final InputStream in = command_exec.getInputStream();
        final String line = readLine(in);
        return Integer.parseInt(line);
    }

    /**
     * Configure sSH client.
     *
     * @param host_name the host _ name
     * @param ssh_port the ssh _ port
     * @param authentication the authentication
     * @throws IOException the iO exception
     */
    protected void configureSSHClient(final String host_name, final int ssh_port, final AuthMethod authentication) throws IOException {

        ssh.setConnectTimeout(SSH_CONNECTION_TIMEOUT);
        ssh.getTransport().setTimeoutMs(SSH_TRANSPORT_TIMEOUT_MILLIS);
        ssh.getConnection().setTimeoutMs(SSH_CHANNEL_CONNECTION_TIMEOUT_MILLIS);
        ssh.loadKnownHosts();
        ssh.addHostKeyVerifier(PROMISCUOUS_HOST_VERIFIER);
        ssh.connect(host_name, ssh_port);
        ssh.auth(Platforms.getCurrentUser(), authentication);
    }
}
