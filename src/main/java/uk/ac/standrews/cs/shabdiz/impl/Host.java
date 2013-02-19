package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.madface.Credentials;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

public abstract class Host {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());
    private final InetAddress address;
    private final boolean local;
    protected final Credentials credentials;

    public Host(final String name, final Credentials credentials) throws IOException {

        this(InetAddress.getByName(name), credentials);
    }

    public Host(final InetAddress address, final Credentials credentials) {

        this.address = address;
        this.credentials = credentials;
        local = NetworkUtil.isValidLocalAddress(address);
    }

    public abstract void upload(File source, String destination) throws IOException;

    public abstract void upload(Collection<File> sources, String destination) throws IOException;

    public abstract void download(String source, File destination) throws IOException;

    public abstract Process execute(String... command) throws IOException;

    public abstract Platform getPlatform() throws IOException;

    public InetAddress getAddress() {

        return address;
    }

    public boolean isLocal() {

        return local;
    }

    public void shutdown() {

        LOGGER.info("shutting down host " + address);
    }

    //    public synchronized Platform getPlatform() throws IOException, InterruptedException {
    //
    //        if (platform == null) {
    //            initialisePlatform();
    //        }
    //        return platform;
    //    }
    //
    //    private void initialisePlatform() throws IOException, InterruptedException {
    //
    //        platform = Platform.getHostPlatform(this);
    //    }

}
