package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.standrews.cs.nds.util.NetworkUtil;

public abstract class HostFactory {

    private static final HostFactory DEFAULT_HOST_FACTORY = new DefaultHostFactory();

    public abstract Host createHost(String host_name, final Credentials credentials) throws IOException;

    public static HostFactory getDefaultHostFactory() {

        return DEFAULT_HOST_FACTORY;
    }
}

class DefaultHostFactory extends HostFactory {

    @Override
    public Host createHost(final String host_name, final Credentials credentials) throws IOException {

        return NetworkUtil.isValidLocalAddress(InetAddress.getByName(host_name)) ? new LocalHost() : new RemoteSSHHost(host_name, credentials);
    }
}
