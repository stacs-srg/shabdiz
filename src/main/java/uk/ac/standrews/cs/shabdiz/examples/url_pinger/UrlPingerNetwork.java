package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.shabdiz.AbstractDeployableNetwork;
import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;

public class UrlPingerNetwork extends AbstractDeployableNetwork<DefaultApplicationDescriptor> {

    public UrlPingerNetwork() {

        super("URL Pinger Network");
    }

    @Override
    public void deploy(final DefaultApplicationDescriptor application_descriptor) throws IOException, InterruptedException, TimeoutException {

        //ignore;

    }
}

class UrlPingerApplicationManager implements ApplicationManager<Void> {

    private final URL target;

    public UrlPingerApplicationManager(final URL target) {

        validateTarget(target);
        this.target = target;
    }

    private void validateTarget(final URL target) {

        if (!target.getProtocol().toLowerCase().equals("http")) { throw new IllegalArgumentException("HTTP urls only"); }
    }

    @Override
    public ApplicationState getApplicationState(final ApplicationDescriptor descriptor) {

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("GET");

            connection.connect();
            final int response_code = connection.getResponseCode();
            switch (response_code) {
                case HttpURLConnection.HTTP_OK:
                    return ApplicationState.RUNNING;
                default:
                    return ApplicationState.UNKNOWN;
            }
        }
        catch (final Exception e) {
            return ApplicationState.UNREACHABLE;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException();

    }

    @Override
    public Void deploy(final Host host) throws Exception {

        throw new UnsupportedOperationException();
    }

}
