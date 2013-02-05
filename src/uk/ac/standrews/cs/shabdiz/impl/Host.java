package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ariabod.barreleye.Channel;
import com.ariabod.barreleye.ChannelType;
import com.ariabod.barreleye.SSHSession;
import com.ariabod.barreleye.SSHSessionFactory;
import com.ariabod.barreleye.exception.SSHException;

public class Host {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());
    private static final int DEFAULT_SSH_PORT = 22;
    private final InetAddress address;
    private final boolean local;
    private final Credentials credentials;
    private final SSHSession session;
    private volatile Platform platform;

    public Host(final String name, final Credentials credentials) throws UnknownHostException, SSHException {

        this.address = InetAddress.getByName(name);
        this.credentials = credentials;
        local = isLocal(address);
        session = createSSHSession(name);
    }

    private SSHSession createSSHSession(final String host_name) throws SSHException {

        final SSHSessionFactory session_factory = SSHSessionFactory.getInstance();
        final SSHSession session = session_factory.createSession(credentials.getUsername(), host_name, DEFAULT_SSH_PORT);
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
