package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;

public class EchoApplicationDescriptor extends DefaultApplicationDescriptor {

    private final AtomicReference<EchoService> application_reference;
    private InetSocketAddress address;

    public EchoApplicationDescriptor(final Host host, final EchoApplicationManager manager) {

        super(host, manager);
        application_reference = new AtomicReference<EchoService>();
    }

    public EchoService getApplicationReference() {

        return application_reference.get();
    }

    void setApplicationReference(final EchoService service) {

        application_reference.set(service);
    }

    public InetSocketAddress getAddress() {

        return address;
    }

    public void setAddress(final InetSocketAddress address) {

        this.address = address;
    }

    @Override
    public String toString() {

        return "Echo Service on [" + (address == null ? "UNDEPLOYED" : address) + "]";
    }
}
