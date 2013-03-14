package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.net.URL;

import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;

class UrlPingerDescriptor extends DefaultApplicationDescriptor {

    private static final UrlPingerManager URL_PINGER_MANAGER = new UrlPingerManager();

    private final URL target;

    public UrlPingerDescriptor(final URL target) {

        super(null, URL_PINGER_MANAGER);
        validateTarget(target);
        this.target = target;
    }

    private void validateTarget(final URL target) {

        if (!target.getProtocol().toLowerCase().equals("http")) { throw new IllegalArgumentException("HTTP urls only"); }
    }

    public URL getTarget() {

        return target;
    }

    @Override
    public int compareTo(final DefaultApplicationDescriptor other) {

        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {

        return target.toString();
    }
}
