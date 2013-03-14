package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Platform;

public abstract class HostWrapper implements Host {

    private final Host unwrapped_host;

    protected HostWrapper(final Host host) {

        this.unwrapped_host = host;
    }

    protected Host getUnwrappedHost() {

        return unwrapped_host;
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        unwrapped_host.upload(sources, destination);
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        unwrapped_host.upload(source, destination);
    }

    @Override
    public void close() throws IOException {

        unwrapped_host.close();
    }

    @Override
    public boolean isLocal() {

        return unwrapped_host.isLocal();
    }

    @Override
    public Platform getPlatform() throws IOException {

        return unwrapped_host.getPlatform();
    }

    @Override
    public InetAddress getAddress() {

        return unwrapped_host.getAddress();
    }

    @Override
    public Process execute(final String... command) throws IOException {

        return unwrapped_host.execute(command);
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        unwrapped_host.download(source, destination);
    }
}
