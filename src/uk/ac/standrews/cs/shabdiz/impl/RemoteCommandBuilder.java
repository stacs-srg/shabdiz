package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ariabod.barreleye.ChannelExec;
import com.ariabod.barreleye.ChannelType;

public class RemoteCommandBuilder implements RemoteProcessBuilder {

    private static final Logger LOGGER = Logger.getLogger(RemoteCommandBuilder.class.getName());
    private volatile String command;

    protected RemoteCommandBuilder(final String command) {

        this.command = command;
    }

    @Override
    public Process start(final Host host) throws IOException {

        return new ChannelExecProcess(host);
    }

    private class ChannelExecProcess extends Process {

        private final PipedOutputStream out;
        private final PipedInputStream in;
        private final PipedInputStream err;
        private final ChannelExec channel;
        private final Host host;

        public ChannelExecProcess(final Host host) throws IOException {

            this.host = host;
            this.channel = host.openSSHChannel(ChannelType.EXEC);
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
            channel.connect();
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
            final ChannelExec kill_channel = host.openSSHChannel(ChannelType.EXEC);
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
