package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;

import uk.ac.standrews.cs.shabdiz.AbstractApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;

public class UrlPingerNetwork extends AbstractApplicationNetwork<DefaultApplicationDescriptor> {

    private static final class PrintNewAndOldStateListener implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent evt) {

            System.out.println("State of " + evt.getSource() + " changed from " + evt.getOldValue() + " to " + evt.getNewValue());
        }
    }

    private static final PrintNewAndOldStateListener PRINT_LISTENER = new PrintNewAndOldStateListener();

    public UrlPingerNetwork() {

        super("URL Pinger Network");
    }

    public static void main(final String[] args) throws MalformedURLException {

        final UrlPingerNetwork network = new UrlPingerNetwork();
        final URL[] targets = {new URL("http://www.google.co.uk"), new URL("http://www.cs.st-andrews.ac.uk"), new URL("http://maven.cs.st-andrews.ac.uk"), new URL("http://www.bbc.co.uk/news/"), new URL("http://quicksilver.hg.cs.st-andrews.ac.uk")};

        configureUrlPingerNetwork(network, targets);
    }

    private static void configureUrlPingerNetwork(final UrlPingerNetwork network, final URL[] targets) throws MalformedURLException {

        for (final URL url : targets) {
            final UrlPingerDescriptor descriptor = createUrlPingerDescriptor(url);
            network.add(descriptor);
        }
    }

    private static UrlPingerDescriptor createUrlPingerDescriptor(final URL url) throws MalformedURLException {

        final UrlPingerDescriptor descriptor = new UrlPingerDescriptor(url);
        descriptor.addPropertyChangeListener(DefaultApplicationDescriptor.STATE_PROPERTY_NAME, PRINT_LISTENER);
        return descriptor;
    }
}
