package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.barreleye.Channel;
import uk.ac.standrews.cs.barreleye.ChannelType;
import uk.ac.standrews.cs.barreleye.SSHClient;
import uk.ac.standrews.cs.barreleye.SSHClientFactory;
import uk.ac.standrews.cs.barreleye.exception.SSHException;


public class Host {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());
    private static final int DEFAULT_SSH_PORT = 22;
    private final InetAddress address;
    private final boolean local;
    private final Credentials credentials;
    private final SSHClient session;
    private volatile Platform platform;

    public Host(final String name, final Credentials credentials) throws UnknownHostException, SSHException {

        this.address = InetAddress.getByName(name);
        this.credentials = credentials;
        local = isLocal(address);
        session = createSSHSession(name);
    }

    private SSHClient createSSHSession(final String host_name) throws SSHException {

        final SSHClientFactory session_factory = SSHClientFactory.getInstance();
        final SSHClient session = session_factory.createSession(credentials.getUsername(), host_name, DEFAULT_SSH_PORT);
        Credentials.setSSHKnownHosts(session_factory);
        return session;
    }

    private static boolean isLocal(final InetAddress address) {

        boolean local = address.isAnyLocalAddress() || address.isLoopbackAddress();
        if (!local) {
            try {
                local = NetworkInterface.getByInetAddress(address) != null;
            }
            catch (final SocketException e) {
                local = false;
            }
        }
        return local;
    }

    public InetAddress getAddress() {

        return address;
    }

    public boolean isLocal() {

        return local;
    }

    public <T extends Channel> T openSSHChannel(final ChannelType type) throws IOException {

        synchronized (session) {
            if (!session.isConnected()) {
                initialiseSession();
            }
        }
        return session.openChannel(type);
    }

    private void initialiseSession() throws IOException {

        if (credentials != null) {
            credentials.authenticate(session);
        }
        session.connect();
    }

    public synchronized Platform getPlatform() throws IOException, InterruptedException {

        if (platform == null) {
            initialisePlatform();
        }
        return platform;
    }

    private void initialisePlatform() throws IOException, InterruptedException {

        platform = Platform.getHostPlatform(this);
    }

    public void shutdown() {

        try {
            session.close();
        }
        catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "unable to close SSH session", e);
        }
    }
}
